package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.domain.request.order.CancelOrderRequest;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành riêng cho các thao tác quản lý đơn hàng phía SELLER.
 *
 * Phân quyền:
 *  - GET /seller/orders            → SELLER (xem danh sách đơn hàng của Shop, lọc theo OrderStatus)
 *  - PUT /seller/orders/{id}/cancel → SELLER (hủy đơn hết hàng đột xuất, hoàn tiền ví & kho)
 */
@RestController
@RequestMapping("/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final OrderService orderService;

    /**
     * GET /seller/orders?status=PROCESSING&page=0&size=10
     * Seller xem danh sách đơn hàng thuộc Shop của mình (có phân trang & lọc theo trạng thái).
     *
     * @param status Trạng thái đơn hàng (PROCESSING, READY_TO_SHIP, SHIPPING, DELIVERED, CANCELLED) - optional
     * @param page   Số trang (0-indexed, mặc định 0)
     * @param size   Số bản ghi mỗi trang (mặc định 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse.OrderSummaryResponse>>> getSellerOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        String email = authentication.getName();
        PagedResponse<OrderResponse.OrderSummaryResponse> response =
                orderService.getManageOrders(email, status, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách đơn hàng của Shop thành công!", response)
        );
    }

    /**
     * PUT /seller/orders/{id}/cancel
     * Seller hủy đơn hàng do hết hàng đột xuất:
     *  - Tự động hoàn tiền vào ví khách (nếu thanh toán qua ví WALLET)
     *  - Tự động hoàn lại số lượng tồn kho (restock) cho các biến thể
     *  - Chuyển trạng thái đơn sang CANCELLED, ghi OrderTracking và gửi Notification cho khách
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> cancelOrderBySeller(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        String reason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : "Người bán hủy đơn do hết hàng đột xuất";

        OrderResponse.OrderDetailResponse response = orderService.cancelOrderBySeller(id, email, reason);

        return ResponseEntity.ok(
                ApiResponse.success("Hủy đơn hàng thành công, đã hoàn kho và tiền ví cho khách!", response)
        );
    }
}
