package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.domain.request.admin.UpdateUserRoleRequest;
import com.shoppy.backend.domain.request.admin.UpdateUserStatusRequest;
import com.shoppy.backend.domain.response.admin.AdminUserDetailResponse;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /admin/users?search=...&role=...&status=...&page=0&size=10
     * Admin xem danh sách tất cả người dùng trên sàn (hỗ trợ tìm kiếm, lọc & phân trang).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        PagedResponse<UserResponse> response = adminUserService.getAllUsers(search, role, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công!", response));
    }

    /**
     * GET /admin/users/{id}
     * Admin xem chi tiết thông tin một người dùng (hồ sơ, số dư ví, shop liên kết, hãng vận chuyển).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(
            @PathVariable Long id
    ) {
        AdminUserDetailResponse response = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết người dùng thành công!", response));
    }

    /**
     * PATCH /admin/users/{id}/status
     * Admin thay đổi trạng thái tài khoản (Khóa BLOCKED, Kích hoạt ACTIVE, Tạm ngưng INACTIVE).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        UserResponse response = adminUserService.updateUserStatus(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công!", response));
    }

    /**
     * PATCH /admin/users/{id}/role
     * Admin thay đổi vai trò tài khoản (CUSTOMER, SELLER, SHIPPER, ADMIN).
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        UserResponse response = adminUserService.updateUserRole(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò người dùng thành công!", response));
    }
}
