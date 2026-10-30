package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.response.category.CategoryResponse;
import com.shoppy.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /categories
     * Public API: Lấy toàn bộ cây danh mục sản phẩm đa cấp (dành cho người mua & người bán).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> tree = categoryService.getAllCategoriesTree();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công!", tree));
    }

    /**
     * GET /categories/{id}
     * Public API: Xem chi tiết một danh mục sản phẩm theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryDetail(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin danh mục thành công!", response));
    }
}
