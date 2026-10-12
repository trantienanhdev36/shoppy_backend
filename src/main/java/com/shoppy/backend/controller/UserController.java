package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.request.shop.RegisterShopRequest;
import com.shoppy.backend.domain.request.user.RegisterRequest;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.entity.User;
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
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ShopService shopService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<User>> register(
            @Valid @ModelAttribute RegisterRequest request,
            // required = false giúp nhận được request kể cả khi KHÔNG truyền key 'avatar'
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        User registeredUser = userService.register(request, avatar);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.", registeredUser));
    }
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Bạn chưa đăng nhập hoặc Token không hợp lệ!"));
        }

        String currentEmail = authentication.getName();
        User user = userService.getUserByEmail(currentEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin người dùng thành công!", user)
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
}