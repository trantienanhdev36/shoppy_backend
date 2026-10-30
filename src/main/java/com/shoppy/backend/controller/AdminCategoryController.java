package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.category.CategoryRequest;
import com.shoppy.backend.domain.request.category.UpdateCategoryStatusRequest;
import com.shoppy.backend.domain.response.category.CategoryResponse;
import com.shoppy.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * GET /admin/categories?search=...&status=...&page=0&size=10
     * Admin: Xem danh sách danh mục phẳng có phân trang, tìm kiếm và lọc trạng thái.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        PagedResponse<CategoryResponse> response = categoryService.getAllCategoriesPaged(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công!", response));
    }

    /**
     * POST /admin/categories (multipart/form-data)
     * Admin: Tạo danh mục mới kèm upload ảnh icon/banner (Body: form-data).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategoryMultipart(
            @Valid @ModelAttribute CategoryRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        CategoryResponse response = categoryService.createCategory(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục mới thành công!", response));
    }

    /**
     * POST /admin/categories (application/json)
     * Admin: Tạo danh mục mới không upload ảnh (Body: raw JSON).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategoryJson(
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse response = categoryService.createCategory(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo danh mục mới thành công!", response));
    }

    /**
     * PUT /admin/categories/{id} (multipart/form-data)
     * Admin: Cập nhật danh mục kèm thay ảnh mới (Body: form-data).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryMultipart(
            @PathVariable Long id,
            @Valid @ModelAttribute CategoryRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request, image);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công!", response));
    }

    /**
     * PUT /admin/categories/{id} (application/json)
     * Admin: Cập nhật thông tin danh mục không đổi ảnh (Body: raw JSON).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryJson(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse response = categoryService.updateCategory(id, request, null);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công!", response));
    }

    /**
     * PATCH /admin/categories/{id}/status
     * Admin: Cập nhật trạng thái hoạt động của danh mục (ACTIVE, INACTIVE).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryStatusRequest request
    ) {
        CategoryResponse response = categoryService.updateCategoryStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái danh mục thành công!", response));
    }

    /**
     * DELETE /admin/categories/{id}
     * Admin: Xóa mềm danh mục sản phẩm (chặn xóa nếu danh mục đang có sản phẩm kinh doanh).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công!"));
    }
}
