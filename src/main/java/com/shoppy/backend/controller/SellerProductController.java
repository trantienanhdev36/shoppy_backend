package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.StockStatus;
import com.shoppy.backend.domain.response.product.ProductDetailResponseDTO;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller dành riêng cho các thao tác quản lý sản phẩm phía SELLER.
 *
 * Phân quyền:
 *  - GET /seller/products       → SELLER (xem danh sách sản phẩm thuộc Shop của mình, lọc tồn kho)
 *  - GET /seller/products/{id}  → SELLER (xem chi tiết 1 sản phẩm thuộc Shop của mình,
 *                                          bao gồm cả sản phẩm/variant đang inactive)
 */
@RestController
@RequestMapping("/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final ProductService productService;

    /**
     * GET /seller/products?stockStatus=IN_STOCK&keyword=...&page=0&size=10
     * Seller xem danh sách sản phẩm thuộc Shop của mình (có phân trang & lọc tồn kho).
     *
     * @param stockStatus Trạng thái kho (ALL, IN_STOCK, OUT_OF_STOCK) - optional
     * @param keyword     Từ khóa tìm kiếm theo tên sản phẩm - optional
     * @param page        Số trang (0-indexed, mặc định 0)
     * @param size        Số bản ghi mỗi trang (mặc định 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponseDTO>>> getMyProducts(
            @RequestParam(required = false) StockStatus stockStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        String email = authentication.getName();
        PagedResponse<ProductResponseDTO> response =
                productService.getSellerProducts(email, stockStatus, keyword, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách sản phẩm của Shop thành công!", response)
        );
    }

    /**
     * GET /seller/products/{id}
     * Seller xem chi tiết 1 sản phẩm thuộc Shop của mình.
     * Khác với public GET /products/{id}:
     *  - Không lọc isActive: seller thấy được cả sản phẩm đang bị ẩn
     *  - Trả toàn bộ variant chưa bị xóa, bao gồm variant đang inactive
     *  - Kiểm tra ownership: sản phẩm phải thuộc Shop của seller
     *
     * @param id             ID sản phẩm cần xem chi tiết
     * @param authentication JWT token của seller
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> getMyProductDetail(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.getSellerProductDetail(email, id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết sản phẩm thành công!", response)
        );
    }
}
