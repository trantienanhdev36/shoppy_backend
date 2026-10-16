package com.shoppy.backend.controller;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.domain.request.order.CreateOrderRequest;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các endpoint liên quan đến Đơn hàng.
 *
 * Phân quyền:
 *  - POST /orders                        → CUSTOMER (đặt đơn)
 *  - GET  /orders/my                     → CUSTOMER (xem đơn của mình)
 *  - GET  /orders/{id}                   → CUSTOMER (chi tiết đơn)
 *  - GET  /orders/manage?status=...      → SELLER / ADMIN
 *  - PUT  /orders/{id}/ready-to-ship     → SELLER / ADMIN
 *  - PUT  /orders/{id}/assign-shipper    → SHIPPER (tự nhận đơn)
 *  - PUT  /orders/{id}/delivered         → SHIPPER (xác nhận giao thành công)
 *  - GET  /orders/shipper/my-deliveries  → SHIPPER
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CUSTOMER ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /orders
     * Khách hàng đặt đơn hàng mới.
     * Trả về ngay lập tức (async processing qua Redis Queue).
     */
    @PostMapping
    public ResponseEntity<OrderResponse.CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        OrderResponse.CreateOrderResponse response = orderService.createOrder(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /orders/my
     * Khách hàng xem danh sách đơn hàng của mình.
     */
    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse.OrderSummaryResponse>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getMyOrders(email));
    }

    /**
     * GET /orders/{id}
     * Khách hàng xem chi tiết một đơn hàng của mình.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse.OrderDetailResponse> getOrderDetail(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getOrderDetail(id, email));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELLER / ADMIN ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /orders/manage?status=PROCESSING
     * Seller / Admin xem danh sách đơn hàng theo trạng thái.
     */
    @GetMapping("/manage")
    public ResponseEntity<List<OrderResponse.OrderSummaryResponse>> getOrdersByStatus(
            @RequestParam(defaultValue = "PROCESSING") OrderStatus status) {

        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    /**
     * PUT /orders/{id}/ready-to-ship
     * Seller xác nhận đã đóng gói, sẵn sàng giao cho shipper.
     */
    @PutMapping("/{id}/ready-to-ship")
    public ResponseEntity<OrderResponse.OrderDetailResponse> markReadyToShip(
            @PathVariable Long id,
            Authentication authentication) {

        // Lấy sellerId từ token (sẽ cần extend để lấy userId từ UserDetails)
        Long sellerId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(orderService.markReadyToShip(id, sellerId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHIPPER ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * PUT /orders/{id}/assign-shipper
     * Shipper tự nhận đơn hàng để giao.
     */
    @PutMapping("/{id}/assign-shipper")
    public ResponseEntity<OrderResponse.OrderDetailResponse> assignShipper(
            @PathVariable Long id,
            Authentication authentication) {

        Long shipperId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(orderService.assignShipper(id, shipperId));
    }

    /**
     * PUT /orders/{id}/delivered
     * Shipper xác nhận giao hàng thành công.
     * Nếu COD → tự động ghi nhận ShipperCodTransaction.
     */
    @PutMapping("/{id}/delivered")
    public ResponseEntity<OrderResponse.OrderDetailResponse> confirmDelivered(
            @PathVariable Long id,
            Authentication authentication) {

        Long shipperId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(orderService.confirmDelivered(id, shipperId));
    }

    /**
     * GET /orders/shipper/my-deliveries?status=SHIPPING
     * Shipper xem danh sách đơn đang giao / đã giao.
     */
    @GetMapping("/shipper/my-deliveries")
    public ResponseEntity<List<OrderResponse.OrderSummaryResponse>> getMyDeliveries(
            @RequestParam(defaultValue = "SHIPPING") OrderStatus status,
            Authentication authentication) {

        Long shipperId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(orderService.getShipperOrders(shipperId, status));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER: Lấy userId từ Authentication Principal
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy userId từ email được lưu trong SecurityContext (principal name).
     * Thực hiện một lần query DB để lấy id của user hiện tại.
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));
    }
}
