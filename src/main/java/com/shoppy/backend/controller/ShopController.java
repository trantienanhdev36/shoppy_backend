package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.request.shop.RegisterShopRequest;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;


    // API Lấy thông tin Shop của User hiện tại
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(Authentication authentication) {
        String email = authentication.getName();
        ShopResponse response = shopService.getMyShop(email);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin Shop thành công!", response)
        );
    }
}