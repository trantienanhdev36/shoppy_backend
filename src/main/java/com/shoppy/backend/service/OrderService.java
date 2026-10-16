package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.*;
import com.shoppy.backend.domain.request.order.CreateOrderRequest;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    // ── Redis Queue key ───────────────────────────────────────────────────────
    public static final String ORDER_QUEUE_KEY = "queue:orders";

    // ── Repositories ──────────────────────────────────────────────────────────
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final ShipperCodTransactionRepository shipperCodTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final ShippingCompanyRepository shippingCompanyRepository;

    // ── Redis ─────────────────────────────────────────────────────────────────
    private final StringRedisTemplate redisTemplate;

    // ═════════════════════════════════════════════════════════════════════════
    // A. CUSTOMER: Tạo đơn hàng (DRAFT → push queue)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Bước 1: Customer gửi request tạo đơn.
     * Hệ thống tạo bản ghi PENDING ngay lập tức và đẩy vào Redis Queue.
     * Worker sẽ xử lý ngầm (kiểm tra kho, thanh toán) trong nền.
     */
    @Transactional
    public OrderResponse.CreateOrderResponse createOrder(String customerEmail, CreateOrderRequest request) {
        // 1.1 Xác định customer
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 1.2 Kiểm tra hãng vận chuyển tồn tại
        ShippingCompany shippingCompany = shippingCompanyRepository.findById(request.getShippingCompanyId())
                .orElseThrow(() -> new RuntimeException("Hãng vận chuyển không tồn tại"));

        // 1.3 Xây dựng danh sách OrderItem (validate variant tồn tại, tính subtotal)
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long shopId = null; // Lấy từ product của variant đầu tiên

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getProductVariantId())
                    .orElseThrow(() -> new RuntimeException(
                            "Biến thể sản phẩm không tồn tại: id=" + itemReq.getProductVariantId()));

            // Lấy shopId từ product (validate tất cả items cùng một shop)
            Long variantShopId = variant.getProduct().getShop().getId();
            if (shopId == null) {
                shopId = variantShopId;
            } else if (!shopId.equals(variantShopId)) {
                throw new RuntimeException("Không thể đặt sản phẩm từ nhiều shop trong cùng một đơn hàng");
            }

            BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .productId(variant.getProduct().getId())
                    .productVariantId(variant.getId())
                    .variantId(variant.getId())              // Legacy column
                    .productImage(variant.getImageUrl())    // Ảnh snapshot
                    // SNAPSHOT fields (cột mới)
                    .productNameSnapshot(variant.getProduct().getName())
                    .variantNameSnapshot(variant.getVariantName())
                    .priceSnapshot(variant.getPrice())
                    // Legacy fields (cột CŨ trong DB - giữ cùng giá trị)
                    .productName(variant.getProduct().getName())
                    .variantName(variant.getVariantName())
                    .unitPrice(variant.getPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build());
        }

        // 1.4 Tạo bản ghi Order với trạng thái PENDING
        Order order = Order.builder()
                .customerId(customer.getId())
                .userId(customer.getId())                // Tương thích cột user_id cũ trong DB
                .shopId(shopId)
                .shippingCompanyId(shippingCompany.getId())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .totalAmount(totalAmount)
                .shippingFee(shippingCompany.getBaseFee())
                .note(request.getNote())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 1.5 Gán order vào items và lưu
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);

        // 1.6 Tạo bản ghi tracking đầu tiên
        addTracking(savedOrder, TrackingStatus.ORDER_PLACED, "Đơn hàng đã được đặt thành công", customer.getId());

        // 1.7 Đẩy orderId vào Redis Queue (RPUSH - xử lý theo thứ tự FIFO)
        redisTemplate.opsForList().rightPush(ORDER_QUEUE_KEY, savedOrder.getId().toString());
        log.info("[OrderService] Đã đẩy orderId [{}] vào Redis Queue '{}'", savedOrder.getId(), ORDER_QUEUE_KEY);

        return OrderResponse.CreateOrderResponse.fromEntity(savedOrder);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // B. WORKER: Xử lý đơn hàng ngầm (kiểm tra kho + thanh toán)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Được gọi bởi OrderQueueWorker sau khi lấy orderId từ Redis Queue.
     * Thực hiện trong một @Transactional để đảm bảo toàn vẹn dữ liệu.
     * Bất kỳ lỗi nào → rollback toàn bộ.
     */
    @Transactional
    public void processOrder(Long orderId) {
        // 2.1 Lấy đơn hàng với PESSIMISTIC_WRITE lock
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("[OrderWorker] Không tìm thấy orderId [{}]", orderId);
            return;
        }

        // Chỉ xử lý đơn ở trạng thái PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[OrderWorker] orderId [{}] không ở trạng thái PENDING (hiện: {}), bỏ qua",
                    orderId, order.getStatus());
            return;
        }

        // 2.2 Tải danh sách items
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        // 2.3 Kiểm tra & trừ tồn kho (PESSIMISTIC_WRITE lock từng variant)
        boolean stockOk = checkAndDeductStock(order, items);
        if (!stockOk) {
            // Thiếu kho → Hủy đơn (stock chưa bị trừ do exception/rollback)
            cancelOrder(order, "Hủy đơn: Một hoặc nhiều sản phẩm không đủ tồn kho");
            return;
        }

        // 2.4 Xử lý thanh toán
        boolean paymentOk = processPayment(order);
        if (!paymentOk) {
            // Thiếu tiền ví → Hủy đơn; kho đã trừ sẽ được rollback bởi @Transactional
            cancelOrder(order, "Hủy đơn: Số dư ví không đủ để thanh toán");
            throw new RuntimeException("Không đủ số dư ví - rollback tồn kho");
        }

        // 2.5 Chuyển trạng thái sang PROCESSING
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.PREPARING,
                "Đơn hàng đã được xác nhận, shop đang chuẩn bị hàng", null);

        log.info("[OrderWorker] Xử lý thành công orderId [{}] → PROCESSING", orderId);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // C. SELLER: Quản lý đơn hàng
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Seller cập nhật trạng thái đơn sang READY_TO_SHIP (đóng gói xong).
     */
    @Transactional
    public OrderResponse.OrderDetailResponse markReadyToShip(Long orderId, Long sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Chỉ có thể chuyển READY_TO_SHIP từ trạng thái PROCESSING");
        }

        order.setStatus(OrderStatus.READY_TO_SHIP);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.READY_FOR_PICKUP,
                "Hàng đã đóng gói xong, sẵn sàng cho shipper lấy", sellerId);

        return buildDetailResponse(order);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // D. SHIPPER: Nhận đơn & Giao hàng
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Shipper thuộc hãng vận chuyển nhận đơn hàng.
     */
    @Transactional
    public OrderResponse.OrderDetailResponse assignShipper(Long orderId, Long shipperId) {
        User shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shipper"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new RuntimeException("Đơn hàng chưa sẵn sàng để giao (trạng thái: " + order.getStatus() + ")");
        }

        // Kiểm tra shipper thuộc đúng hãng vận chuyển của đơn
        if (shipper.getShippingCompanyId() == null ||
                !shipper.getShippingCompanyId().equals(order.getShippingCompanyId())) {
            throw new RuntimeException("Shipper không thuộc hãng vận chuyển của đơn hàng này");
        }

        order.setShipperId(shipperId);
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.PICKED_UP,
                "Shipper đã nhận đơn và bắt đầu giao hàng", shipperId);

        log.info("[OrderService] Shipper [{}] đã nhận đơn [{}]", shipperId, orderId);
        return buildDetailResponse(order);
    }

    /**
     * Shipper xác nhận giao hàng thành công.
     * Nếu là COD → tạo ShipperCodTransaction (COLLECTION) để đối soát tài chính.
     */
    @Transactional
    public OrderResponse.OrderDetailResponse confirmDelivered(Long orderId, Long shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (!shipperId.equals(order.getShipperId())) {
            throw new RuntimeException("Bạn không phải shipper được giao đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Đơn hàng không ở trạng thái đang giao");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.DELIVERED, "Giao hàng thành công", shipperId);

        // Nếu COD → Ghi nhận bản ghi thu tiền mặt cho shipper
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            BigDecimal codAmount = order.getTotalAmount().add(order.getShippingFee());
            ShipperCodTransaction codTx = ShipperCodTransaction.builder()
                    .orderId(order.getId())
                    .shipperId(shipperId)
                    .type(ShipperTransactionType.COLLECTION)
                    .amount(codAmount)
                    .note("Thu tiền mặt COD đơn hàng #" + orderId)
                    .build();
            shipperCodTransactionRepository.save(codTx);
            log.info("[OrderService] Đã tạo COD Transaction: shipper [{}] thu [{}] VND từ đơn [{}]",
                    shipperId, codAmount, orderId);
        }

        return buildDetailResponse(order);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // E. QUERY: Lấy thông tin đơn hàng
    // ═════════════════════════════════════════════════════════════════════════

    /** Lấy chi tiết một đơn hàng (customer xem đơn của mình) */
    public OrderResponse.OrderDetailResponse getOrderDetail(Long orderId, String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + orderId));

        if (!order.getCustomerId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        return buildDetailResponse(order);
    }

    /** Lấy danh sách đơn hàng của customer */
    public List<OrderResponse.OrderSummaryResponse> getMyOrders(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(OrderResponse.OrderSummaryResponse::fromEntity)
                .toList();
    }

    /** Seller/Admin xem đơn theo trạng thái */
    public List<OrderResponse.OrderSummaryResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(OrderResponse.OrderSummaryResponse::fromEntity)
                .toList();
    }

    /** Shipper xem danh sách đơn đang giao */
    public List<OrderResponse.OrderSummaryResponse> getShipperOrders(Long shipperId, OrderStatus status) {
        return orderRepository.findByShipperIdAndStatusOrderByCreatedAtDesc(shipperId, status)
                .stream()
                .map(OrderResponse.OrderSummaryResponse::fromEntity)
                .toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra và trừ tồn kho với PESSIMISTIC_WRITE lock.
     * @return true nếu tất cả sản phẩm đủ hàng và đã trừ thành công
     */
    private boolean checkAndDeductStock(Order order, List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductVariant variant = productVariantRepository
                    .findByIdWithLock(item.getProductVariantId())
                    .orElse(null);

            if (variant == null) {
                log.warn("[OrderWorker] Variant [{}] không tồn tại, hủy đơn [{}]",
                        item.getProductVariantId(), order.getId());
                return false;
            }

            if (variant.getStockQuantity() < item.getQuantity()) {
                log.warn("[OrderWorker] Variant [{}] thiếu kho: cần {}, còn {} → hủy đơn [{}]",
                        variant.getId(), item.getQuantity(), variant.getStockQuantity(), order.getId());
                return false;
            }

            // Trừ kho
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            productVariantRepository.save(variant);
        }
        return true;
    }

    /**
     * Xử lý thanh toán dựa trên phương thức.
     * @return true nếu thanh toán thành công hoặc là COD
     */
    private boolean processPayment(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            // COD: không cần trừ tiền ngay, bỏ qua bước này
            log.info("[OrderWorker] Đơn [{}] thanh toán COD, bỏ qua bước trừ ví", order.getId());
            return true;
        }

        // WALLET: kiểm tra và trừ số dư ví
        User customer = userRepository.findById(order.getCustomerId()).orElse(null);
        if (customer == null) return false;

        BigDecimal grandTotal = order.getTotalAmount().add(order.getShippingFee());
        BigDecimal currentBalance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;

        if (currentBalance.compareTo(grandTotal) < 0) {
            log.warn("[OrderWorker] Ví customer [{}] không đủ: cần {}, có {} → hủy đơn [{}]",
                    customer.getId(), grandTotal, currentBalance, order.getId());
            return false;
        }

        // Trừ tiền ví
        customer.setBalance(currentBalance.subtract(grandTotal));
        userRepository.save(customer);

        addTracking(order, TrackingStatus.PAYMENT_CONFIRMED,
                String.format("Đã thanh toán %,.0f VND qua ví điện tử", grandTotal.doubleValue()), null);
        log.info("[OrderWorker] Đã trừ {} VND ví của customer [{}] cho đơn [{}]",
                grandTotal, customer.getId(), order.getId());
        return true;
    }

    /** Hủy đơn hàng và ghi tracking */
    private void cancelOrder(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.CANCELLED, reason, null);
        log.info("[OrderWorker] Đã hủy đơn [{}]: {}", order.getId(), reason);
    }

    /** Tạo bản ghi OrderTracking */
    private void addTracking(Order order, TrackingStatus status, String description, Long updatedBy) {
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(status)
                .description(description)
                .updatedBy(updatedBy)
                .build();
        orderTrackingRepository.save(tracking);
    }

    /** Build OrderDetailResponse (load lazy collections) */
    private OrderResponse.OrderDetailResponse buildDetailResponse(Order order) {
        // Tải items và trackings để tránh LazyInitializationException
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<com.shoppy.backend.entity.OrderTracking> trackings =
                orderTrackingRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
        order.setItems(items);
        order.setTrackings(trackings);
        return OrderResponse.OrderDetailResponse.fromEntity(order);
    }
}
