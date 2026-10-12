package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.request.product.CreateProductRequest;
import com.shoppy.backend.domain.response.product.ProductDetailResponseDTO;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(value="/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            Authentication authentication,
            @Valid @ModelAttribute CreateProductRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "variantImages", required = false) List<MultipartFile> variantImages
    ) {
        String email = authentication.getName();
        ProductResponseDTO response = productService.createProduct(email, request, images, variantImages);

        return ResponseEntity.ok(
                ApiResponse.success("Thêm sản phẩm thành công!", response)
        );
    }

    @GetMapping(value = "/getAll")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getAllProducts() {
        List<ProductResponseDTO> products = productService.getAllProducts();

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách sản phẩm thành công!", products)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> getProductById(@PathVariable Long id) {
        ProductDetailResponseDTO response = productService.getProductById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết sản phẩm thành công!", response)
        );
    }
}