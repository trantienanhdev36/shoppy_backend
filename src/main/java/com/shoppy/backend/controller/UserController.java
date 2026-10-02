package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.dto.user.RegisterRequest;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<User>> register(
            @Valid @ModelAttribute RegisterRequest request,
            // 👈 required = false giúp nhận được request kể cả khi KHÔNG truyền key 'avatar'
            @RequestParam(value = "avatar", required = false) MultipartFile avatar
    ) {
        User registeredUser = userService.register(request, avatar);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.", registeredUser));
    }


}