package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.admin.UpdateShopStatusRequest;
import com.shoppy.backend.domain.response.admin.AdminShopDetailResponse;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.service.AdminShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private final AdminShopService adminShopService;

    /**
     * GET /admin/shops?search=...&status=...&page=0&size=10
     * Admin xem danh sách tất cả Cửa hàng trên sàn (tìm kiếm theo tên, lọc trạng thái, phân trang).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ShopResponse>>> getAllShops(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        PagedResponse<ShopResponse> response = adminShopService.getAllShops(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Cửa hàng thành công!", response));
    }

    /**
     * GET /admin/shops/{id}
     * Admin xem thông tin chi tiết một Cửa hàng (kèm thông tin Chủ shop, số lượng sản phẩm & đơn hàng).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminShopDetailResponse>> getShopDetail(@PathVariable Long id) {
        AdminShopDetailResponse response = adminShopService.getShopDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết Cửa hàng thành công!", response));
    }

    /**
     * PATCH /admin/shops/{id}/status
     * Admin thay đổi trạng thái hoạt động Cửa hàng (ACTIVE, INACTIVE, BLOCKED).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShopStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShopStatusRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        ShopResponse response = adminShopService.updateShopStatus(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái Cửa hàng thành công!", response));
    }
}
