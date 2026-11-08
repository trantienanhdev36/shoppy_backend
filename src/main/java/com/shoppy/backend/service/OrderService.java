package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.*;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.order.CreateOrderRequest;
import com.shoppy.backend.domain.response.order.CreateOrderGroupResponse;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
    private final ShopBalanceService shopBalanceService;

    // ── Redis ─────────────────────────────────────────────────────────────────
    private final StringRedisTemplate redisTemplate;

    // ═════════════════════════════════════════════════════════════════════════
    // A. CUSTOMER: Tạo đơn hàng (DRAFT → push queue)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Bước 1: Customer gửi request tạo đơn.
     * Hệ thống tạo bản ghi PENDING ngay lập tức và đẩy vào Redis Queue.
     * Worker sẽ xử lý ngầm (kiểm tra kho, thanh toán) trong nền.
     * <p>
     * Hỗ trợ đặt hàng từ NHIỀU shop trong cùng 1 request:
     * - 1 shop  → 1 Order được tạo, orderGroupId = null
     * - N shops → N Order được tạo, orderGroupId = "GRP-YYYYMMDD-XXXXXXXX"
     */
    @Transactional
    public CreateOrderGroupResponse createOrder(String customerEmail, CreateOrderRequest request) {
        // 1.1 Xác định customer
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1.2 Kiểm tra hãng vận chuyển tồn tại và đang hoạt động (ACTIVE)
        ShippingCompany shippingCompany = shippingCompanyRepository.findByIdAndIsDeletedFalse(request.getShippingCompanyId())
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(shippingCompany.getStatus())) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_NOT_ACTIVE);
        }

        // 1.3 Validate từng item và phân nhóm theo shopId (giữ nguyên thứ tự bằng LinkedHashMap)
        // Map<shopId, List<(itemReq, variant)>>
        Map<Long, List<OrderItem>> itemsByShop = new LinkedHashMap<>();

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = productVariantRepository.findById(itemReq.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            Product product = variant.getProduct();
            if (Boolean.TRUE.equals(product.getIsDeleted()) || Boolean.FALSE.equals(product.getIsActive())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            // Kiểm tra Cửa hàng sở hữu: Cửa hàng phải đang ACTIVE
            Shop productShop = product.getShop();
            if (productShop == null || Boolean.TRUE.equals(productShop.getIsDeleted())
                    || !"ACTIVE".equalsIgnoreCase(productShop.getStatus())) {
                throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
            }

            // Kiểm tra Danh mục sản phẩm: nếu có danh mục thì phải đang ACTIVE
            Category category = product.getCategory();
            if (category != null && (Boolean.TRUE.equals(category.getIsDeleted())
                    || !"ACTIVE".equalsIgnoreCase(category.getStatus()))) {
                throw new AppException(ErrorCode.CATEGORY_NOT_ACTIVE);
            }

            BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productVariantId(variant.getId())
                    .variantId(variant.getId())              // Legacy column
                    .productImage(variant.getImageUrl())    // Ảnh snapshot
                    .productNameSnapshot(product.getName())
                    .variantNameSnapshot(variant.getVariantName())
                    .priceSnapshot(variant.getPrice())
                    .productName(product.getName())          // Legacy
                    .variantName(variant.getVariantName())   // Legacy
                    .unitPrice(variant.getPrice())           // Legacy
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            itemsByShop.computeIfAbsent(productShop.getId(), k -> new ArrayList<>()).add(orderItem);
        }

        // 1.4 Sinh orderGroupId nếu có nhiều hơn 1 shop
        String orderGroupId = itemsByShop.size() > 1
                ? "GRP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                  + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                : null;

        // 1.5 Tạo 1 Order per shop, lưu items, tracking, enqueue
        List<Order> createdOrders = new ArrayList<>();
        List<Integer> itemCountPerOrder = new ArrayList<>();

        for (Map.Entry<Long, List<OrderItem>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<OrderItem> shopItems = entry.getValue();

            BigDecimal shopTotal = shopItems.stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Order order = Order.builder()
                    .customerId(customer.getId())
                    .userId(customer.getId())
                    .shopId(shopId)
                    .shippingCompanyId(shippingCompany.getId())
                    .paymentMethod(request.getPaymentMethod())
                    .status(OrderStatus.PENDING)
                    .shippingAddress(request.getShippingAddress())
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .totalAmount(shopTotal)
                    .shippingFee(shippingCompany.getBaseFee())
                    .note(request.getNote())
                    .orderGroupId(orderGroupId)
                    .build();

            Order savedOrder = orderRepository.save(order);

            // Gán order vào từng item rồi lưu
            for (OrderItem item : shopItems) {
                item.setOrder(savedOrder);
            }
            orderItemRepository.saveAll(shopItems);

            // Tracking đầu tiên
            addTracking(savedOrder, TrackingStatus.ORDER_PLACED, "Đơn hàng đã được đặt thành công", customer.getId());

            // Đẩy vào Redis Queue
            enqueueOrderAfterCommit(savedOrder.getId());
            log.info("[OrderService] Đã đẩy orderId [{}] vào Redis Queue '{}' (nhóm: {})",
                    savedOrder.getId(), ORDER_QUEUE_KEY, orderGroupId != null ? orderGroupId : "single");

            // Thông báo cho khách hàng
            notificationService.sendNotification(
                    customer.getId(),
                    NotificationType.ORDER_PLACED,
                    "Đặt hàng thành công",
                    "Đơn hàng #" + savedOrder.getId() + " của bạn đã được đặt thành công.",
                    savedOrder.getId()
            );

            // Thông báo cho người bán
            shopRepository.findById(shopId).ifPresent(shop ->
                    notificationService.sendNotification(
                            shop.getUserId(),
                            NotificationType.NEW_ORDER_FOR_SELLER,
                            "Có đơn hàng mới",
                            "Shop của bạn vừa nhận được đơn hàng mới #" + savedOrder.getId() + ".",
                            savedOrder.getId()
                    )
            );

            createdOrders.add(savedOrder);
            itemCountPerOrder.add(shopItems.size());
        }

        // 1.6 Tính tổng toàn bộ nhóm đơn
        BigDecimal grandTotalAmount = createdOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotalShipping = createdOrders.stream()
                .map(Order::getShippingFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1.7 Build response
        List<CreateOrderGroupResponse.OrderSummary> summaries = new ArrayList<>();
        for (int i = 0; i < createdOrders.size(); i++) {
            summaries.add(CreateOrderGroupResponse.OrderSummary
                    .fromEntity(createdOrders.get(i), itemCountPerOrder.get(i)));
        }

        return CreateOrderGroupResponse.builder()
                .orderGroupId(orderGroupId)
                .totalOrders(createdOrders.size())
                .totalAmount(grandTotalAmount)
                .totalShippingFee(grandTotalShipping)
                .grandTotal(grandTotalAmount.add(grandTotalShipping))
                .orders(summaries)
                .build();
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
     * Kiểm tra quyền sở hữu: Seller chỉ được đóng gói đơn thuộc Shop của mình (Admin có toàn quyền).
     */
    @Transactional
    public OrderResponse.OrderDetailResponse markReadyToShip(Long orderId, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra quyền sở hữu đơn hàng: Nếu không phải ADMIN thì bắt buộc phải là chủ Shop
        if (seller.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(seller.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!order.getShopId().equals(shop.getId())) {
                throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_SHOP);
            }
        }

        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new AppException(ErrorCode.ORDER_WRONG_STATUS);
        }

        // Tự động sinh mã vận đơn khi người bán đóng gói xong (READY_TO_SHIP)
        if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
            order.setTrackingNumber(generateTrackingNumber(order));
        }

        order.setStatus(OrderStatus.READY_TO_SHIP);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.READY_FOR_PICKUP,
                "Hàng đã đóng gói xong, sẵn sàng cho shipper lấy. Mã vận đơn: " + order.getTrackingNumber(), seller.getId());

        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng sẵn sàng giao",
                "Đơn hàng #" + order.getId() + " đã được đóng gói và sẵn sàng giao cho shipper. Mã vận đơn: " + order.getTrackingNumber(),
                order.getId()
        );

        return buildDetailResponse(order);
    }

    /** Overload giữ lại tương thích nếu gọi bằng sellerId */
    @Transactional
    public OrderResponse.OrderDetailResponse markReadyToShip(Long orderId, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return markReadyToShip(orderId, seller.getEmail());
    }

    /**
     * Seller hủy đơn hàng do hết hàng đột xuất.
     * Quy trình xử lý:
     * 1. Kiểm tra quyền sở hữu đơn hàng (thuộc Shop của Seller hoặc ADMIN).
     * 2. Kiểm tra trạng thái đơn: Không được hủy nếu đã SHIPPING, DELIVERED hoặc CANCELLED.
     * 3. Hoàn lại số lượng tồn kho (restock) cho các biến thể trong đơn.
     * 4. Tự động hoàn tiền vào ví khách nếu đơn thanh toán bằng WALLET.
     * 5. Cập nhật trạng thái đơn thành CANCELLED, thêm OrderTracking và gửi thông báo cho khách.
     *
     * @param orderId     ID đơn hàng cần hủy
     * @param sellerEmail Email của Seller thực hiện hủy
     * @param reason      Lý do hủy đơn (ví dụ: "Hết hàng đột xuất")
     * @return            Chi tiết đơn hàng sau khi hủy
     */
    @Transactional
    public OrderResponse.OrderDetailResponse cancelOrderBySeller(Long orderId, String sellerEmail, String reason) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // 1. Kiểm tra quyền sở hữu đơn hàng
        if (seller.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(seller.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!order.getShopId().equals(shop.getId())) {
                throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_SHOP);
            }
        }

        // 2. Kiểm tra trạng thái đơn hàng: Không thể hủy đơn đang giao hoặc đã giao/đã hủy
        if (order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_WRONG_STATUS);
        }

        // 3. Hoàn lại tồn kho cho các sản phẩm/biến thể
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        restoreStock(order, items);

        // 4. Hoàn tiền nếu đơn thanh toán qua ví WALLET và đã trừ tiền
        boolean refunded = false;
        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            boolean isDebitCompleted = walletTransactionRepository.findByOrderId(order.getId())
                    .map(tx -> tx.getStatus() == TransactionStatus.COMPLETED)
                    .orElse(false);

            if (isDebitCompleted || order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.READY_TO_SHIP) {
                BigDecimal refundAmount = order.getTotalAmount().add(
                        order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO
                );
                walletService.refundOrder(order.getCustomerId(), order.getId(), refundAmount, reason);
                refunded = true;
            }
        }

        // 5. Cập nhật trạng thái và lưu DB
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        String trackingNote = "Người bán đã hủy đơn hàng. Lý do: " + reason + (refunded ? " (Đã hoàn tiền về ví)" : "");
        addTracking(order, TrackingStatus.CANCELLED, trackingNote, seller.getId());

        // 6. Gửi thông báo in-app cho khách hàng
        notificationService.sendNotification(
                order.getCustomerId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Đơn hàng #" + order.getId() + " đã bị hủy",
                "Đơn hàng #" + order.getId() + " đã bị người bán hủy. Lý do: " + reason
                        + (refunded ? ". Tiền thanh toán đã được hoàn vào ví Shoppy của bạn." : "."),
                order.getId()
        );

        log.info("[OrderService] Seller [{}] đã hủy đơn [{}]. Lý do: {}. Hoàn tiền ví: {}",
                sellerEmail, orderId, reason, refunded);

        return buildDetailResponse(order);
    }

    /**
     * Customer hủy đơn hàng của mình.
     * Chỉ cho phép hủy khi người bán CHƯA chuẩn bị xong đơn hàng (trước trạng thái READY_TO_SHIP).
     *
     * Quy trình xử lý:
     * 1. Xác thực tài khoản Customer qua email.
     * 2. Kiểm tra quyền sở hữu đơn hàng (đơn phải thuộc về Customer này).
     * 3. Kiểm tra trạng thái đơn:
     *    - CANCELLED: ném lỗi ORDER_ALREADY_CANCELLED.
     *    - READY_TO_SHIP, SHIPPING, DELIVERED: ném lỗi ORDER_ALREADY_PREPARED (đã chuẩn bị xong hoặc đang giao, không được hủy).
     *    - PENDING, WAITING_FOR_PAYMENT, PROCESSING: hợp lệ để hủy.
     * 4. Hoàn lại số lượng tồn kho (restock):
     *    - Nếu đơn ở WAITING_FOR_PAYMENT hoặc PROCESSING: đã bị trừ kho -> hoàn kho.
     *    - Nếu đơn ở PENDING: chưa bị trừ kho -> không cần hoàn.
     * 5. Tự động hoàn tiền vào ví khách (nếu đơn thanh toán bằng WALLET và đã bị trừ tiền).
     * 6. Cập nhật trạng thái đơn thành CANCELLED, thêm OrderTracking.
     * 7. Gửi thông báo in-app cho Khách hàng và Người bán (Shop Owner).
     *
     * @param orderId       ID đơn hàng cần hủy
     * @param customerEmail Email của Khách hàng thực hiện hủy
     * @param reason        Lý do hủy đơn
     * @return              Chi tiết đơn hàng sau khi hủy
     */
    @Transactional
    public OrderResponse.OrderDetailResponse cancelOrderByCustomer(Long orderId, String customerEmail, String reason) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // 1. Kiểm tra quyền sở hữu đơn hàng: chỉ khách hàng đặt đơn mới được hủy theo luồng này
        if (!order.getCustomerId().equals(customer.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_CUSTOMER);
        }

        // 2. Kiểm tra trạng thái đơn hàng
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }

        // Người dùng chỉ được hủy khi người bán chưa chuẩn bị xong (READY_TO_SHIP, SHIPPING, DELIVERED đều không được phép)
        if (order.getStatus() == OrderStatus.READY_TO_SHIP
                || order.getStatus() == OrderStatus.SHIPPING
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.ORDER_ALREADY_PREPARED);
        }

        // 3. Hoàn lại tồn kho nếu đơn đã ở trạng thái giữ kho / trừ kho (PROCESSING hoặc WAITING_FOR_PAYMENT)
        if (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.WAITING_FOR_PAYMENT) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            restoreStock(order, items);
        }

        // 4. Hoàn tiền nếu đơn thanh toán qua ví WALLET và đã trừ tiền
        boolean refunded = false;
        if (order.getPaymentMethod() == PaymentMethod.WALLET) {
            boolean isDebitCompleted = walletTransactionRepository.findByOrderId(order.getId())
                    .map(tx -> tx.getStatus() == TransactionStatus.COMPLETED)
                    .orElse(false);

            if (isDebitCompleted || order.getStatus() == OrderStatus.PROCESSING) {
                BigDecimal refundAmount = order.getTotalAmount().add(
                        order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO
                );
                walletService.refundOrder(order.getCustomerId(), order.getId(), refundAmount, "Khách hàng hủy đơn: " + reason);
                refunded = true;
            }
        }

        // 5. Cập nhật trạng thái và lưu DB
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        String trackingNote = "Khách hàng đã hủy đơn hàng. Lý do: " + reason + (refunded ? " (Đã hoàn tiền về ví)" : "");
        addTracking(order, TrackingStatus.CANCELLED, trackingNote, customer.getId());

        // 6. Gửi thông báo in-app
        // 6.1 Gửi cho khách hàng
        notificationService.sendNotification(
                customer.getId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Hủy đơn hàng thành công",
                "Đơn hàng #" + order.getId() + " của bạn đã được hủy thành công."
                        + (refunded ? " Tiền đã được hoàn về ví Shoppy của bạn." : ""),
                order.getId()
        );

        // 6.2 Gửi cho Người bán (Chủ Shop)
        shopRepository.findById(order.getShopId()).ifPresent(shop -> {
            notificationService.sendNotification(
                    shop.getUserId(),
                    NotificationType.ORDER_STATUS_CHANGED,
                    "Đơn hàng #" + order.getId() + " đã bị khách hủy",
                    "Khách hàng đã hủy đơn hàng #" + order.getId() + ". Lý do: " + reason,
                    order.getId()
            );
        });

        log.info("[OrderService] Customer [{}] đã hủy đơn [{}]. Lý do: {}. Hoàn tiền ví: {}",
                customerEmail, orderId, reason, refunded);

        return buildDetailResponse(order);
    }

    /**
     * Phương thức điều hướng hủy đơn thông minh:
     * - Nếu người dùng là Khách hàng đã đặt đơn -> Thực hiện hủy theo luồng Customer.
     * - Nếu người dùng là Người bán quản lý Shop (hoặc Admin) -> Thực hiện hủy theo luồng Seller.
     */
    @Transactional
    public OrderResponse.OrderDetailResponse cancelOrder(Long orderId, String userEmail, String reason) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getCustomerId().equals(user.getId())) {
            return cancelOrderByCustomer(orderId, userEmail, reason);
        } else {
            return cancelOrderBySeller(orderId, userEmail, reason);
        }
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
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!shipperId.equals(order.getShipperId())) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ASSIGNED);
        }

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.ORDER_WRONG_STATUS);
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.DELIVERED, "Giao hàng thành công", shipperId);
        shopBalanceService.creditWalletOrder(order);

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

    /**
     * Lấy chi tiết một đơn hàng.
     * Cho phép:
     *  - Customer: Xem đơn hàng của chính mình.
     *  - Seller: Xem đơn hàng thuộc Shop của mình.
     *  - Admin: Có toàn quyền xem bất kỳ đơn hàng nào.
     */
    public OrderResponse.OrderDetailResponse getOrderDetail(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        boolean isCustomer = order.getCustomerId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isShopOwner = false;

        if (!isCustomer && !isAdmin && user.getRole() == UserRole.SELLER) {
            isShopOwner = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .map(shop -> shop.getId().equals(order.getShopId()))
                    .orElse(false);
        }

        if (!isCustomer && !isAdmin && !isShopOwner) {
            throw new AppException(ErrorCode.ORDER_NOT_BELONG_TO_SHOP);
        }

        return buildDetailResponse(order);
    }

    /**
     * Lấy danh sách đơn hàng của customer — có phân trang.
     * Nếu {@code page} vượt quá tổng số trang → tự động clamp về trang cuối.
     *
     * @param customerEmail Email của customer hiện tại
     * @param page          Số trang (0-indexed)
     * @param size          Số bản ghi mỗi trang
     */
    public PagedResponse<OrderResponse.OrderSummaryResponse> getMyOrders(
            String customerEmail, int page, int size) {

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> orderRepository.findByCustomerId(customer.getId(), pageable),
                OrderResponse.OrderSummaryResponse::fromEntity
        );
    }

    /**
     * Lấy danh sách đơn hàng cho Seller / Admin quản lý — có phân trang.
     * - Nếu là ADMIN: Xem tất cả đơn trên toàn hệ thống (lọc theo status nếu có).
     * - Nếu là SELLER: Chỉ xem các đơn thuộc về Shop của chính mình (lọc theo status nếu có).
     *
     * @param email   Email của người quản lý (Seller hoặc Admin)
     * @param status  Trạng thái đơn hàng cần lọc (optional, nếu null sẽ lấy tất cả trạng thái)
     * @param page    Số trang (0-indexed)
     * @param size    Số bản ghi mỗi trang
     */
    public PagedResponse<OrderResponse.OrderSummaryResponse> getManageOrders(
            String email, OrderStatus status, int page, int size) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        Long shopId = null;

        if (!isAdmin) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
            shopId = shop.getId();
        }

        final Long targetShopId = shopId;

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> {
                    if (isAdmin) {
                        return status != null
                                ? orderRepository.findByStatus(status, pageable)
                                : orderRepository.findAll(pageable);
                    } else {
                        return status != null
                                ? orderRepository.findByShopIdAndStatus(targetShopId, status, pageable)
                                : orderRepository.findByShopId(targetShopId, pageable);
                    }
                },
                OrderResponse.OrderSummaryResponse::fromEntity
        );
    }

    /** Seller/Admin xem đơn theo trạng thái (toàn sàn - deprecated) */
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
        // Tải items và trackings riêng biệt để tránh LazyInitializationException và không làm dereference collection của JPA Entity
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<com.shoppy.backend.entity.OrderTracking> trackings =
                orderTrackingRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
        return OrderResponse.OrderDetailResponse.fromEntity(order, items, trackings);
    }

    /**
     * Tự động sinh mã vận đơn cho đơn hàng.
     * Cấu trúc: [MÃ_HÃNG_VẬN_CHUYỂN][YYMMDD][ORDER_ID_PADDED][RANDOM_SUFFIX]
     * Ví dụ: GHN26110100012A8F3
     */
    private String generateTrackingNumber(Order order) {
        String companyCode = "SPY";
        if (order.getShippingCompanyId() != null) {
            companyCode = shippingCompanyRepository.findById(order.getShippingCompanyId())
                    .map(ShippingCompany::getCode)
                    .filter(c -> c != null && !c.isBlank())
                    .map(String::toUpperCase)
                    .orElse("SPY");
        }

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String orderPart = String.format("%05d", order.getId() != null ? order.getId() : 0);
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();

        return companyCode + datePart + orderPart + randomSuffix;
    }
}
