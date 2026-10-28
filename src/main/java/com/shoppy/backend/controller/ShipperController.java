package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.domain.request.shipper.CodRemittanceRequest;
import com.shoppy.backend.domain.response.order.OrderResponse;
import com.shoppy.backend.domain.response.shipper.CodBalanceResponse;
import com.shoppy.backend.service.ShipperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ dành riêng cho SHIPPER.
 *
 * Phân quyền:
 *  - GET  /shipper/orders/available              → SHIPPER (xem đơn sẵn sàng để nhận)
 *  - GET  /shipper/orders/{id}                   → SHIPPER (chi tiết 1 đơn hàng)
 *  - PUT  /shipper/orders/{id}/assign            → SHIPPER (nhận đơn)
 *  - PUT  /shipper/orders/{id}/delivered         → SHIPPER (xác nhận giao hàng thành công)
 *  - GET  /shipper/orders/my-deliveries          → SHIPPER (đơn hàng của mình)
 *  - GET  /shipper/cod/transactions              → SHIPPER (lịch sử COD)
 *  - GET  /shipper/cod/balance                   → SHIPPER (tổng kết số dư COD)
 *  - POST /shipper/cod/remittance                → SHIPPER (nộp tiền COD về công ty)
 */
@RestController
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    // ─────────────────────────────────────────────────────────────────────────
    // ĐƠN HÀNG
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /shipper/orders/available
     * Lấy danh sách đơn READY_TO_SHIP thuộc hãng vận chuyển của shipper.
     */
    @GetMapping("/orders/available")
    public ResponseEntity<ApiResponse<List<OrderResponse.OrderSummaryResponse>>> getAvailableOrders(
            Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse.OrderSummaryResponse> orders = shipperService.getAvailableOrders(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng sẵn sàng thành công!", orders));
    }

    /**
     * GET /shipper/orders/{id}
     * Xem chi tiết 1 đơn hàng (chỉ xem được đơn thuộc hãng của mình).
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> getOrderDetail(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        OrderResponse.OrderDetailResponse order = shipperService.getOrderDetail(email, id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công!", order));
    }

    /**
     * PUT /shipper/orders/{id}/assign
     * Shipper nhận đơn hàng để giao (READY_TO_SHIP → SHIPPING).
     */
    @PutMapping("/orders/{id}/assign")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> assignOrder(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        OrderResponse.OrderDetailResponse order = shipperService.assignShipper(email, id);
        return ResponseEntity.ok(ApiResponse.success("Nhận đơn hàng thành công!", order));
    }

    /**
     * PUT /shipper/orders/{id}/delivered
     * Shipper xác nhận giao hàng thành công (SHIPPING → DELIVERED).
     * Tự động tạo COD COLLECTION record nếu đơn là COD.
     */
    @PutMapping("/orders/{id}/delivered")
    public ResponseEntity<ApiResponse<OrderResponse.OrderDetailResponse>> confirmDelivered(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        OrderResponse.OrderDetailResponse order = shipperService.confirmDelivered(email, id);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận giao hàng thành công!", order));
    }

    /**
     * GET /shipper/orders/my-deliveries?status=SHIPPING
     * Xem danh sách đơn hàng của bản thân. Nếu không truyền status → lấy tất cả.
     */
    @GetMapping("/orders/my-deliveries")
    public ResponseEntity<ApiResponse<List<OrderResponse.OrderSummaryResponse>>> getMyDeliveries(
            @RequestParam(required = false) OrderStatus status,
            Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse.OrderSummaryResponse> orders = shipperService.getMyDeliveries(email, status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng của tôi thành công!", orders));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COD TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /shipper/cod/transactions?type=COLLECTION
     * Lấy lịch sử giao dịch COD. Nếu không truyền type → lấy tất cả (COLLECTION + REMITTANCE).
     */
    @GetMapping("/cod/transactions")
    public ResponseEntity<ApiResponse<List<OrderResponse.ShipperCodTransactionResponse>>> getMyCodTransactions(
            @RequestParam(required = false) ShipperTransactionType type,
            Authentication authentication) {
        String email = authentication.getName();
        List<OrderResponse.ShipperCodTransactionResponse> transactions =
                shipperService.getMyCodTransactions(email, type);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch COD thành công!", transactions));
    }

    /**
     * GET /shipper/cod/balance
     * Xem tổng kết số dư COD (tổng thu - tổng nộp = số đang giữ).
     */
    @GetMapping("/cod/balance")
    public ResponseEntity<ApiResponse<CodBalanceResponse>> getMyCodBalance(Authentication authentication) {
        String email = authentication.getName();
        CodBalanceResponse balance = shipperService.getMyCodBalance(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy số dư COD thành công!", balance));
    }

    /**
     * POST /shipper/cod/remittance
     * Shipper nộp tiền COD về công ty vận chuyển (tạo bản ghi REMITTANCE).
     */
    @PostMapping("/cod/remittance")
    public ResponseEntity<ApiResponse<OrderResponse.ShipperCodTransactionResponse>> submitRemittance(
            @Valid @RequestBody CodRemittanceRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        OrderResponse.ShipperCodTransactionResponse tx = shipperService.submitCodRemittance(email, request);
        return ResponseEntity.ok(ApiResponse.success("Nộp tiền COD về công ty thành công!", tx));
    }
}
