package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.shop.UpdateShopRequest;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.domain.response.shop.ShopPublicProfileResponse;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.service.ProductService;
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
    private final ProductService productService;

    /**
     * GET /shops/me
     * Lấy thông tin Shop của Seller hiện tại.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(Authentication authentication) {
        String email = authentication.getName();
        ShopResponse response = shopService.getMyShop(email);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin Shop thành công!", response)
        );
    }

    /**
     * PUT /shops/me (Multipart Form Data)
     * Cập nhật thông tin Shop (tên, mô tả, địa chỉ, logo mới upload).
     */
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShopMultipart(
            Authentication authentication,
            @Valid @ModelAttribute UpdateShopRequest request,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) {
        String email = authentication.getName();
        ShopResponse response = shopService.updateMyShop(email, request, logo);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thông tin Cửa hàng thành công!", response)
        );
    }

    /**
     * PUT /shops/me (JSON)
     * Cập nhật thông tin Shop mà không kèm upload file logo mới.
     */
    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShopJson(
            Authentication authentication,
            @Valid @RequestBody UpdateShopRequest request
    ) {
        String email = authentication.getName();
        ShopResponse response = shopService.updateMyShop(email, request, null);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thông tin Cửa hàng thành công!", response)
        );
    }

    /**
     * GET /shops/{id} (Public)
     * Khách mua hàng xem thông tin chi tiết Cửa hàng và danh sách sản phẩm Shop đang bán (có phân trang).
     *
     * @param id   ID của Cửa hàng
     * @param page Số trang sản phẩm (0-indexed, mặc định 0)
     * @param size Số bản ghi sản phẩm mỗi trang (mặc định 10)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopPublicProfileResponse>> getPublicShopProfile(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        ShopPublicProfileResponse response = shopService.getPublicShopProfile(id, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin Cửa hàng thành công!", response)
        );
    }

    /**
     * GET /shops/{id}/products (Public)
     * Khách phân trang xem riêng danh sách sản phẩm của Cửa hàng.
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponseDTO>>> getPublicShopProducts(
            @PathVariable Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        PagedResponse<ProductResponseDTO> response = productService.getPublicProductsByShop(id, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách sản phẩm của Cửa hàng thành công!", response)
        );
    }
}