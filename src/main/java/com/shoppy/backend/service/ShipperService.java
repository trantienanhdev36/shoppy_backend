package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.common.enums.TrackingStatus;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.domain.request.shipper.CodRemittanceRequest;
import com.shoppy.backend.domain.request.shipper.RegisterShipperRequest;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.domain.response.shipper.CodBalanceResponse;
import com.shoppy.backend.domain.response.shipper.ShipperOrderDetailResponse;
import com.shoppy.backend.domain.response.shipper.ShipperOrderSummaryResponse;
import com.shoppy.backend.domain.response.shipper.ShipperResponse;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipperService {

    private final UserRepository userRepository;
    private final ShippingCompanyRepository shippingCompanyRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTrackingRepository orderTrackingRepository;
    private final ShipperCodTransactionRepository shipperCodTransactionRepository;
    private final ShopRepository shopRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. Xem đơn hàng READY_TO_SHIP có thể nhận (theo hãng vận chuyển)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách đơn hàng đang chờ shipper nhận (READY_TO_SHIP),
     * chỉ trả về đơn thuộc hãng vận chuyển của shipper hiện tại.
     */
    @Transactional(readOnly = true)
    public List<ShipperOrderSummaryResponse> getAvailableOrders(String shipperEmail) {
        User shipper = getShipperByEmail(shipperEmail);
        validateShipperHasCompany(shipper);

        List<Order> orders = orderRepository.findByShippingCompanyIdAndStatusOrderByCreatedAtAsc(
                shipper.getShippingCompanyId(), OrderStatus.READY_TO_SHIP);
        return mapToShipperOrderSummaries(orders);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. Xem chi tiết 1 đơn hàng
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shipper xem chi tiết một đơn hàng.
     * Chỉ được xem đơn thuộc hãng vận chuyển của mình.
     */
    @Transactional(readOnly = true)
    public ShipperOrderDetailResponse getOrderDetail(String shipperEmail, Long orderId) {
        User shipper = getShipperByEmail(shipperEmail);
        validateShipperHasCompany(shipper);

        Order order = orderRepository
                .findByIdAndShippingCompanyId(orderId, shipper.getShippingCompanyId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderResponse.OrderDetailResponse detail = buildDetailResponse(order);
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return ShipperOrderDetailResponse.fromEntity(detail, shop.getAddress());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. Nhận đơn hàng (READY_TO_SHIP → SHIPPING)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shipper nhận đơn hàng để giao.
     * Validate: đơn phải READY_TO_SHIP, shipper phải thuộc đúng hãng vận chuyển.
     */
    @Transactional
    public OrderResponse.OrderDetailResponse assignShipper(String shipperEmail, Long orderId) {
        User shipper = getShipperByEmail(shipperEmail);
        validateShipperHasCompany(shipper);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(ErrorCode.ORDER_WRONG_STATUS);
        }

        if (!shipper.getShippingCompanyId().equals(order.getShippingCompanyId())) {
            throw new AppException(ErrorCode.SHIPPER_WRONG_COMPANY);
        }

        order.setShipperId(shipper.getId());
        order.setStatus(OrderStatus.SHIPPING);
        orderRepository.save(order);

        addTracking(order, TrackingStatus.PICKED_UP,
                "Shipper đã nhận đơn và bắt đầu giao hàng", shipper.getId());

        log.info("[ShipperService] Shipper [{}] đã nhận đơn [{}]", shipper.getId(), orderId);
        return buildDetailResponse(order);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. Xác nhận giao hàng thành công (SHIPPING → DELIVERED)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shipper xác nhận giao hàng thành công.
     * Nếu COD → tạo bản ghi COLLECTION trong ShipperCodTransaction.
     */
    @Transactional
    public OrderResponse.OrderDetailResponse confirmDelivered(String shipperEmail, Long orderId) {
        User shipper = getShipperByEmail(shipperEmail);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!shipper.getId().equals(order.getShipperId())) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ASSIGNED);
        }

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.ORDER_WRONG_STATUS);
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
        addTracking(order, TrackingStatus.DELIVERED, "Giao hàng thành công", shipper.getId());

        // Nếu COD → ghi nhận COLLECTION
        if (order.getPaymentMethod().name().equals("COD")) {
            BigDecimal codAmount = order.getTotalAmount().add(order.getShippingFee());
            ShipperCodTransaction codTx = ShipperCodTransaction.builder()
                    .orderId(order.getId())
                    .shipperId(shipper.getId())
                    .type(ShipperTransactionType.COLLECTION)
                    .status(TransactionStatus.COMPLETED)
                    .amount(codAmount)
                    .note("Thu tiền mặt COD đơn hàng #" + orderId)
                    .build();
            shipperCodTransactionRepository.save(codTx);
            log.info("[ShipperService] COD thu từ khách: {} VND, đơn [{}]", codAmount, orderId);
        }

        log.info("[ShipperService] Shipper [{}] xác nhận giao thành công đơn [{}]", shipper.getId(), orderId);
        return buildDetailResponse(order);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. Xem lịch sử COD transactions của shipper
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy toàn bộ lịch sử giao dịch COD của shipper (COLLECTION + REMITTANCE).
     * Có thể lọc theo type nếu truyền vào.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse.ShipperCodTransactionResponse> getMyCodTransactions(
            String shipperEmail, ShipperTransactionType type) {
        User shipper = getShipperByEmail(shipperEmail);

        List<ShipperCodTransaction> transactions;
        if (type != null) {
            transactions = shipperCodTransactionRepository
                    .findByShipperIdAndTypeOrderByCreatedAtDesc(shipper.getId(), type);
        } else {
            transactions = shipperCodTransactionRepository
                    .findByShipperIdOrderByCreatedAtDesc(shipper.getId());
        }

        return transactions.stream()
                .map(OrderResponse.ShipperCodTransactionResponse::fromEntity)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6. Xem tổng kết số dư COD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tổng hợp số dư COD của shipper:
     * currentBalance = totalCollected (COLLECTION) - totalRemitted (REMITTANCE)
     */
    @Transactional(readOnly = true)
    public CodBalanceResponse getMyCodBalance(String shipperEmail) {
        User shipper = getShipperByEmail(shipperEmail);
        Long shipperId = shipper.getId();

        BigDecimal totalCollected = shipperCodTransactionRepository
                .sumAmountByShipperIdAndType(shipperId, ShipperTransactionType.COLLECTION);
        BigDecimal totalRemitted = shipperCodTransactionRepository
                .sumAmountByShipperIdAndType(shipperId, ShipperTransactionType.REMITTANCE);

        BigDecimal currentBalance = totalCollected.subtract(totalRemitted);

        return CodBalanceResponse.builder()
                .totalCollected(totalCollected)
                .totalRemitted(totalRemitted)
                .currentBalance(currentBalance)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7. Nộp tiền COD về công ty (tạo REMITTANCE)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shipper tạo bản ghi REMITTANCE khi nộp tiền mặt COD về công ty vận chuyển.
     */
    @Transactional
    public OrderResponse.ShipperCodTransactionResponse submitCodRemittance(
            String shipperEmail, CodRemittanceRequest request) {
        User shipper = getShipperByEmail(shipperEmail);

        // Kiểm tra số dư hiện tại đủ để nộp
        BigDecimal totalCollected = shipperCodTransactionRepository
                .sumAmountByShipperIdAndType(shipper.getId(), ShipperTransactionType.COLLECTION);
        BigDecimal totalRemitted = shipperCodTransactionRepository
                .sumAmountByShipperIdAndType(shipper.getId(), ShipperTransactionType.REMITTANCE);
        BigDecimal currentBalance = totalCollected.subtract(totalRemitted);

        if (request.getAmount().compareTo(currentBalance) > 0) {
            throw new IllegalArgumentException(
                    "Số tiền nộp (" + request.getAmount() + " VND) vượt quá số dư đang giữ (" + currentBalance + " VND)");
        }

        String note = request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote()
                : "Shipper nộp tiền COD về công ty";

        ShipperCodTransaction remittance = ShipperCodTransaction.builder()
                .shipperId(shipper.getId())
                .type(ShipperTransactionType.REMITTANCE)
                .status(TransactionStatus.COMPLETED)
                .amount(request.getAmount())
                .note(note)
                .build();
        ShipperCodTransaction saved = shipperCodTransactionRepository.save(remittance);

        log.info("[ShipperService] Shipper [{}] nộp {} VND về công ty", shipper.getId(), request.getAmount());
        return OrderResponse.ShipperCodTransactionResponse.fromEntity(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. Xem danh sách đơn hàng của bản thân (đang giao / đã giao)
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ShipperOrderSummaryResponse> getMyDeliveries(
            String shipperEmail, OrderStatus status) {
        User shipper = getShipperByEmail(shipperEmail);

        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByShipperIdAndStatusOrderByCreatedAtDesc(shipper.getId(), status);
        } else {
            orders = orderRepository.findByShipperIdOrderByCreatedAtDesc(shipper.getId());
        }

        return mapToShipperOrderSummaries(orders);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8. Đăng ký làm Shipper (dành cho Customer đã đăng nhập)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Khách hàng (CUSTOMER) đăng ký trở thành nhân viên giao hàng của một hãng vận chuyển.
     * Quy tắc:
     *  - User phải tồn tại.
     *  - Nếu đã là SHIPPER -> lỗi ALREADY_A_SHIPPER.
     *  - Phải là CUSTOMER (SELLER hoặc ADMIN không được chuyển thành SHIPPER) -> lỗi ONLY_CUSTOMER_CAN_REGISTER_SHIPPER.
     *  - Hãng vận chuyển phải tồn tại và đang ACTIVE.
     *  - Nâng cấp role thành SHIPPER và gán shippingCompany.
     */
    @Transactional
    public ShipperResponse registerShipper(String email, RegisterShipperRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.SHIPPER) {
            throw new AppException(ErrorCode.ALREADY_A_SHIPPER);
        }

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new AppException(ErrorCode.ONLY_CUSTOMER_CAN_REGISTER_SHIPPER);
        }

        ShippingCompany company = shippingCompanyRepository.findById(request.getShippingCompanyId())
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(company.getStatus())) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_NOT_ACTIVE);
        }

        user.setRole(UserRole.SHIPPER);
        user.setShippingCompany(company);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        log.info("[ShipperService] User [{}] đã đăng ký thành công làm Shipper cho hãng [{}]",
                savedUser.getId(), company.getName());

        return ShipperResponse.fromEntity(savedUser);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private User getShipperByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateShipperHasCompany(User shipper) {
        if (shipper.getShippingCompanyId() == null) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_REQUIRED);
        }

        ShippingCompany company = shippingCompanyRepository.findByIdAndIsDeletedFalse(shipper.getShippingCompanyId())
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        if (!"ACTIVE".equalsIgnoreCase(company.getStatus())) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_NOT_ACTIVE);
        }
    }

    private OrderResponse.OrderDetailResponse buildDetailResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderTracking> trackings = orderTrackingRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
        return OrderResponse.OrderDetailResponse.fromEntity(order, items, trackings);
    }

    private List<ShipperOrderSummaryResponse> mapToShipperOrderSummaries(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> shopIds = orders.stream()
                .map(Order::getShopId)
                .distinct()
                .toList();
        Map<Long, Shop> shopsById = shopRepository.findAllById(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, shop -> shop));

        return orders.stream()
                .map(order -> {
                    Shop shop = shopsById.get(order.getShopId());
                    if (shop == null) {
                        throw new AppException(ErrorCode.SHOP_NOT_FOUND);
                    }
                    return ShipperOrderSummaryResponse.fromEntity(
                            order, shop.getAddress());
                })
                .toList();
    }

    private void addTracking(Order order, TrackingStatus status, String description, Long updatedBy) {
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(status)
                .description(description)
                .updatedBy(updatedBy)
                .build();
        orderTrackingRepository.save(tracking);
    }
}
