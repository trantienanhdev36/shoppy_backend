package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.domain.request.order.CancelOrderRequest;
import com.shoppy.backend.domain.request.order.CreateOrderRequest;
import com.shoppy.backend.domain.response.order.CreateOrderGroupResponse;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * Hỗ trợ đặt hàng từ nhiều shop cùng lúc — mỗi shop sẽ được tách thành 1 đơn riêng.
     * Trả về ngay lập tức (async processing qua Redis Queue).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderGroupResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        CreateOrderGroupResponse response = orderService.createOrder(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công! Đơn hàng đang được xử lý.", response));
    }

    /**
     * GET /orders/my?page=0&size=10
     * Khách hàng xem danh sách đơn hàng của mình (có phân trang).
     * Nếu page vượt quá tổng số trang → tự động trả về trang cuối.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse.OrderSummaryResponse>>> getMyOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        String email = authentication.getName();
        PagedResponse<OrderResponse.OrderSummaryResponse> result = orderService.getMyOrders(email, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công!", result));
    }

    /**
     * GET /orders/{id}
     * Xem chi tiết một đơn hàng (Khách xem đơn mình, Seller xem đơn Shop mình, Admin xem toàn quyền).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> getOrderDetail(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        OrderResponse.OrderDetailResponse response = orderService.getOrderDetail(id, email);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết đơn hàng thành công!", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELLER / ADMIN ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /orders/manage?status=PROCESSING&page=0&size=10
     * Seller xem danh sách đơn hàng thuộc Shop của mình (Admin xem toàn bộ hệ thống).
     * Hỗ trợ lọc theo trạng thái và phân trang (last page clamp).
     */
    @GetMapping("/manage")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse.OrderSummaryResponse>>> getManageOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        String email = authentication.getName();
        PagedResponse<OrderResponse.OrderSummaryResponse> response =
                orderService.getManageOrders(email, status, page, size);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng quản lý thành công!", response));
    }

    /**
     * PUT /orders/{id}/ready-to-ship
     * Seller xác nhận đã đóng gói, sẵn sàng giao cho shipper.
     * Kiểm tra bắt buộc: Đơn hàng phải thuộc Shop của Seller đang đăng nhập (hoặc Admin).
     */
    @PutMapping("/{id}/ready-to-ship")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> markReadyToShip(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        OrderResponse.OrderDetailResponse response = orderService.markReadyToShip(id, email);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đóng gói hoàn tất! Đơn hàng sẵn sàng để shipper lấy hàng.", response));
    }

    /**
     * PUT /orders/{id}/cancel
     * Hủy đơn hàng:
     * - Khách hàng (CUSTOMER): Hủy đơn của mình khi Shop chưa chuẩn bị xong (trước READY_TO_SHIP).
     * - Người bán (SELLER) / Admin: Hủy đơn thuộc Shop của mình (hết hàng đột xuất).
     * Tự động hoàn lại tồn kho và hoàn tiền vào ví nếu đã thanh toán.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        String reason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : "Khách hàng hủy đơn hàng";

        OrderResponse.OrderDetailResponse response = orderService.cancelOrder(id, email, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công!", response));
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
