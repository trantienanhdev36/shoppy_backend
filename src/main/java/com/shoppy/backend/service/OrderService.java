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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;

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
        enqueueOrderAfterCommit(savedOrder.getId());
        log.info("[OrderService] Đã đẩy orderId [{}] vào Redis Queue '{}'", savedOrder.getId(), ORDER_QUEUE_KEY);

        // 1.8 Gửi thông báo in-app cho khách hàng & người bán
        notificationService.sendNotification(
                customer.getId(),
                NotificationType.ORDER_PLACED,
                "Đặt hàng thành công",
                "Đơn hàng #" + savedOrder.getId() + " của bạn đã được đặt thành công.",
                savedOrder.getId()
        );

        if (shopId != null) {
            shopRepository.findById(shopId).ifPresent(shop -> {
                notificationService.sendNotification(
                        shop.getUserId(),
                        NotificationType.NEW_ORDER_FOR_SELLER,
                        "Có đơn hàng mới",
                        "Shop của bạn vừa nhận được đơn hàng mới #" + savedOrder.getId() + ".",
                        savedOrder.getId()
                );
            });
        }

        return OrderResponse.CreateOrderResponse.fromEntity(savedOrder);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // B. WORKER: Xử lý đơn hàng ngầm (kiểm tra kho + thanh toán)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Được gọi bởi OrderQueueWorker sau khi lấy orderId từ Redis Queue.
     * Mỗi lần gọi xử lý một giai đoạn; đơn thanh toán ví được tiếp tục sau khi wallet queue hoàn tất.
     */
    @Transactional
    public void processOrder(Long orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("[OrderWorker] Không tìm thấy orderId [{}]", orderId);
            return;
        }

        if (order.getStatus() == OrderStatus.WAITING_FOR_PAYMENT) {
            finishWalletPayment(order);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("[OrderWorker] Bỏ qua orderId [{}], trạng thái hiện tại: {}",
                    orderId, order.getStatus());
            return;
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        boolean stockOk = checkAndDeductStock(order, items);
        if (!stockOk) {
            cancelOrder(order, "Hủy đơn: Một hoặc nhiều sản phẩm không đủ tồn kho");
            return;
        }

        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            order.setStatus(OrderStatus.WAITING_FOR_PAYMENT);
            orderRepository.save(order);
            BigDecimal grandTotal = order.getTotalAmount().add(order.getShippingFee());
            walletService.createDebitRequest(order.getCustomerId(), order.getId(), grandTotal);
            log.info("[OrderWorker] Đơn [{}] đã giữ tồn kho và chờ wallet queue thanh toán", orderId);
            return;
        }

        completeOrderProcessing(order, false);
    }

    @Transactional(readOnly = true)
    public void requeueOrdersNeedingWork() {
        orderRepository.findByStatus(OrderStatus.PENDING)
                .forEach(order -> enqueueOrderAfterCommit(order.getId()));

        orderRepository.findByStatus(OrderStatus.WAITING_FOR_PAYMENT).forEach(order ->
                walletTransactionRepository.findByOrderId(order.getId())
                        .filter(transaction -> transaction.getStatus() != TransactionStatus.PENDING)
                        .ifPresent(transaction -> enqueueOrderAfterCommit(order.getId())));
    }

    private void finishWalletPayment(Order order) {
        WalletTransaction transaction = walletTransactionRepository.findByOrderId(order.getId())
                .orElse(null);
        if (transaction == null || transaction.getStatus() == TransactionStatus.PENDING) {
            return;
        }

        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            restoreStock(order, orderItemRepository.findByOrderId(order.getId()));
            cancelOrder(order, "Hủy đơn: Giao dịch thanh toán ví thất bại");
            return;
        }

        addTracking(order, TrackingStatus.PAYMENT_CONFIRMED,
                String.format("Đã thanh toán %,.0f VND qua ví điện tử",
                        transaction.getAmount().doubleValue()), null);
        completeOrderProcessing(order, true);
    }

    private void completeOrderProcessing(Order order, boolean walletPayment) {
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.PREPARING,
                "Đơn hàng đã được xác nhận, shop đang chuẩn bị hàng", null);
        log.info("[OrderWorker] Xử lý thành công orderId [{}] → PROCESSING (walletPayment={})",
                order.getId(), walletPayment);

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng đã được xác nhận",
                "Đơn hàng #" + order.getId() + " đã được xác nhận thành công và shop đang chuẩn bị hàng.",
                order.getId()
        );
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

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng sẵn sàng giao",
                "Đơn hàng #" + order.getId() + " đã được đóng gói và sẵn sàng giao cho shipper.",
                order.getId()
        );

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

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng đang được giao",
                "Đơn hàng #" + order.getId() + " đang được shipper giao đến bạn.",
                order.getId()
        );

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

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng đã giao thành công",
                "Đơn hàng #" + order.getId() + " đã được giao thành công.",
                order.getId()
        );

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

    /** Lock and validate every requested variant before changing any stock. */
    private boolean checkAndDeductStock(Order order, List<OrderItem> items) {
        Map<Long, Integer> quantities = aggregateQuantities(items);
        Map<Long, ProductVariant> lockedVariants = new TreeMap<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = productVariantRepository
                    .findByIdWithLock(entry.getKey())
                    .orElse(null);

            if (variant == null) {
                log.warn("[OrderWorker] Variant [{}] không tồn tại, hủy đơn [{}]",
                        entry.getKey(), order.getId());
                return false;
            }

            if (variant.getStockQuantity() < entry.getValue()) {
                log.warn("[OrderWorker] Variant [{}] thiếu kho: cần {}, còn {} → hủy đơn [{}]",
                        variant.getId(), entry.getValue(), variant.getStockQuantity(), order.getId());
                return false;
            }

            lockedVariants.put(entry.getKey(), variant);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = lockedVariants.get(entry.getKey());
            variant.setStockQuantity(variant.getStockQuantity() - entry.getValue());
            productVariantRepository.save(variant);
        }
        return true;
    }

    private void restoreStock(Order order, List<OrderItem> items) {
        Map<Long, Integer> quantities = aggregateQuantities(items);
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = productVariantRepository.findByIdWithLock(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy biến thể " + entry.getKey() + " để hoàn kho đơn " + order.getId()));
            variant.setStockQuantity(variant.getStockQuantity() + entry.getValue());
            productVariantRepository.save(variant);
        }
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderItem> items) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItem item : items) {
            quantities.merge(item.getProductVariantId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }

    private void enqueueOrderAfterCommit(Long orderId) {
        Runnable enqueue = () -> {
            String queuedKey = ORDER_QUEUE_KEY + ":queued:" + orderId;
            try {
                Boolean claimed = redisTemplate.opsForValue()
                        .setIfAbsent(queuedKey, "1", Duration.ofMinutes(1));
                if (!Boolean.TRUE.equals(claimed)) {
                    return;
                }
                redisTemplate.opsForList().rightPush(ORDER_QUEUE_KEY, orderId.toString());
            } catch (RuntimeException e) {
                redisTemplate.delete(queuedKey);
                log.error("[OrderService] Không thể đưa orderId [{}] vào queue; worker recovery sẽ thử lại",
                        orderId, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
        } else {
            enqueue.run();
        }
    }

    /** Hủy đơn hàng và ghi tracking */
    private void cancelOrder(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.CANCELLED, reason, null);
        log.info("[OrderWorker] Đã hủy đơn [{}]: {}", order.getId(), reason);

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng đã bị hủy",
                "Đơn hàng #" + order.getId() + " đã bị hủy. Lý do: " + reason,
                order.getId()
        );
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
