package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.request.shipper.RegisterShipperRequest;
import com.shoppy.backend.domain.request.shop.RegisterShopRequest;
import com.shoppy.backend.domain.request.user.ChangePasswordRequest;
import com.shoppy.backend.domain.request.user.RegisterRequest;
import com.shoppy.backend.domain.request.user.UpdateProfileRequest;
import com.shoppy.backend.domain.response.shipper.ShipperResponse;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.service.ShipperService;
import com.shoppy.backend.service.ShopService;
import com.shoppy.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ShopService shopService;
    private final ShipperService shipperService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @ModelAttribute RegisterRequest request,
            // required = false giúp nhận được request kể cả khi KHÔNG truyền key 'avatar'
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        User registeredUser = userService.register(request, avatar);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Yêu cầu đăng ký được ghi nhận! Vui lòng kiểm tra email để kích hoạt tài khoản.", UserResponse.fromEntity(registeredUser)));
    }
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn chưa đăng nhập hoặc Token không hợp lệ!"));
        }

        String currentEmail = authentication.getName();
        User user = userService.getUserByEmail(currentEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin người dùng thành công!", UserResponse.fromEntity(user))
        );
    }

    /**
     * PUT /users/profile (Multipart Form Data)
     * Cập nhật thông tin hồ sơ cá nhân kèm upload file avatar mới.
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfileMultipart(
            Authentication authentication,
            @Valid @ModelAttribute UpdateProfileRequest request,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        String email = authentication.getName();
        User updatedUser = userService.updateProfile(email, request, avatar);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật hồ sơ cá nhân thành công!", UserResponse.fromEntity(updatedUser))
        );
    }

    /**
     * PUT /users/profile (JSON)
     * Cập nhật thông tin hồ sơ cá nhân (họ tên, số điện thoại) không upload avatar.
     */
    @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfileJson(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String email = authentication.getName();
        User updatedUser = userService.updateProfile(email, request, null);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật hồ sơ cá nhân thành công!", UserResponse.fromEntity(updatedUser))
        );
    }

    /**
     * PUT /users/change-password
     * Đổi mật khẩu tài khoản người dùng hiện tại.
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        String email = authentication.getName();
        userService.changePassword(email, request);

        return ResponseEntity.ok(
                ApiResponse.success("Đổi mật khẩu thành công!", null)
        );
    }

    @PostMapping(value = "/shops/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShopResponse>> registerShop(
            Authentication authentication,
            @Valid @ModelAttribute RegisterShopRequest request,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) {
        String email = authentication.getName();
        ShopResponse response = shopService.registerShop(email, request, logo);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng ký Shop thành công!", response)
        );
    }

    @PostMapping("/shippers/register")
    public ResponseEntity<ApiResponse<ShipperResponse>> registerShipper(
            Authentication authentication,
            @Valid @RequestBody RegisterShipperRequest request
    ) {
        String email = authentication.getName();
        ShipperResponse response = shipperService.registerShipper(email, request);

        return ResponseEntity.ok(
                ApiResponse.success("Đăng ký làm Shipper thành công!", response)
        );
    }
}