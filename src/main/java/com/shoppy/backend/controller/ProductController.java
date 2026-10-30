package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.product.AddProductVariantRequest;
import com.shoppy.backend.domain.request.product.CreateProductRequest;
import com.shoppy.backend.domain.request.product.ProductStatusRequest;
import com.shoppy.backend.domain.request.product.UpdateProductRequest;
import com.shoppy.backend.domain.request.product.UpdateVariantStockRequest;
import com.shoppy.backend.domain.response.product.ProductDetailResponseDTO;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.domain.response.product.ProductVariantDTO;
import com.shoppy.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    /**
     * PUT /products/{id} (Multipart Form Data)
     * Cập nhật thông tin sản phẩm, danh mục, giá, kho, ảnh và biến thể.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> updateProductMultipart(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @ModelAttribute UpdateProductRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "variantImages", required = false) List<MultipartFile> variantImages
    ) {
        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.updateProduct(email, id, request, images, variantImages);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật sản phẩm thành công!", response)
        );
    }

    /**
     * PUT /products/{id} (JSON)
     * Cập nhật thông tin sản phẩm, danh mục, giá, kho không kèm upload file mới.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> updateProductJson(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.updateProduct(email, id, request, null, null);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/variants/{variantId}/stock
     * Cập nhật nhanh số lượng tồn kho của 1 biến thể.
     */
    @PatchMapping("/variants/{variantId}/stock")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateVariantStock(
            @PathVariable Long variantId,
            Authentication authentication,
            @Valid @RequestBody UpdateVariantStockRequest request
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.updateVariantStock(email, variantId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật số lượng tồn kho thành công!", response)
        );
    }

    /**
     * PATCH /products/variants/{variantId}/deactivate
     * Tạm ngưng / ẩn một biến thể sản phẩm.
     */
    @PatchMapping("/variants/{variantId}/deactivate")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> deactivateVariant(
            @PathVariable Long variantId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.deactivateVariant(email, variantId);

        return ResponseEntity.ok(
                ApiResponse.success("Tạm ẩn biến thể sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/variants/{variantId}/activate
     * Kích hoạt lại biến thể sản phẩm.
     */
    @PatchMapping("/variants/{variantId}/activate")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> activateVariant(
            @PathVariable Long variantId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.activateVariant(email, variantId);

        return ResponseEntity.ok(
                ApiResponse.success("Kích hoạt lại biến thể sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/variants/{variantId}/status
     * Cập nhật trạng thái hoạt động linh hoạt của biến thể (active: true/false).
     */
    @PatchMapping("/variants/{variantId}/status")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateVariantStatus(
            @PathVariable Long variantId,
            Authentication authentication,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.updateVariantActiveStatus(email, variantId, request.getActive());

        String message = Boolean.TRUE.equals(request.getActive())
                ? "Kích hoạt lại biến thể sản phẩm thành công!"
                : "Tạm ẩn biến thể sản phẩm thành công!";

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }

    /**
     * DELETE /products/variants/{variantId}
     * Xóa mềm một biến thể sản phẩm.
     */
    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<Void>> softDeleteVariant(
            @PathVariable Long variantId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        productService.softDeleteVariant(email, variantId);

        return ResponseEntity.ok(
                ApiResponse.success("Xóa biến thể sản phẩm thành công!", null)
        );
    }

    /**
     * POST /products/{id}/variants (JSON)
     * Thêm nhanh 1 biến thể mới vào sản phẩm (không kèm upload file ảnh).
     */
    @PostMapping(value = "/{id}/variants", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ProductVariantDTO>> addVariantJson(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AddProductVariantRequest request
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.addVariant(email, id, request, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Thêm biến thể sản phẩm thành công!", response)
        );
    }

    /**
     * POST /products/{id}/variants (Multipart Form Data)
     * Thêm nhanh 1 biến thể mới vào sản phẩm kèm upload file ảnh biến thể.
     */
    @PostMapping(value = "/{id}/variants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductVariantDTO>> addVariantMultipart(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @ModelAttribute AddProductVariantRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        String email = authentication.getName();
        ProductVariantDTO response = productService.addVariant(email, id, request, image);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Thêm biến thể sản phẩm thành công!", response)
        );
    }

    /**
     * Lấy danh sách sản phẩm có phân trang.
     *
     * @param page Số trang (0-indexed, mặc định 0)
     * @param size Số bản ghi mỗi trang (mặc định 5)
     *
     * Ví dụ: GET /products/getAll?page=0&size=5
     *        GET /products/getAll?page=1&size=10
     */
    @GetMapping(value = "/getAll")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponseDTO>>> getAllProducts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "2") int size
    ) {
        PagedResponse<ProductResponseDTO> response = productService.getAllProducts(page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách sản phẩm thành công!", response)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> getProductById(@PathVariable Long id) {
        ProductDetailResponseDTO response = productService.getProductById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết sản phẩm thành công!", response)
        );
    }

    /**
     * GET /products/{id}/variants
     * Lấy danh sách các biến thể của sản phẩm (dùng để xem nhanh danh sách biến thể, ID biến thể, số lượng kho).
     */
    @GetMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantDTO>>> getProductVariants(@PathVariable Long id) {
        List<ProductVariantDTO> response = productService.getProductVariants(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách biến thể sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/{id}/deactivate
     * Tạm ngưng kinh doanh / ẩn sản phẩm.
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> deactivateProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.deactivateProduct(email, id);

        return ResponseEntity.ok(
                ApiResponse.success("Tạm ẩn sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/{id}/activate
     * Kích hoạt lại sản phẩm đã bị ẩn.
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> activateProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.activateProduct(email, id);

        return ResponseEntity.ok(
                ApiResponse.success("Kích hoạt lại sản phẩm thành công!", response)
        );
    }

    /**
     * PATCH /products/{id}/status
     * Cập nhật trạng thái hoạt động linh hoạt (active: true/false).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductDetailResponseDTO>> updateProductStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        String email = authentication.getName();
        ProductDetailResponseDTO response = productService.updateProductActiveStatus(email, id, request.getActive());

        String message = Boolean.TRUE.equals(request.getActive())
                ? "Kích hoạt lại sản phẩm thành công!"
                : "Tạm ẩn sản phẩm thành công!";

        return ResponseEntity.ok(
                ApiResponse.success(message, response)
        );
    }

    /**
     * DELETE /products/{id}
     * Xóa mềm sản phẩm (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteProduct(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        productService.softDeleteProduct(email, id);

        return ResponseEntity.ok(
                ApiResponse.success("Xóa sản phẩm thành công!", null)
        );
    }
}