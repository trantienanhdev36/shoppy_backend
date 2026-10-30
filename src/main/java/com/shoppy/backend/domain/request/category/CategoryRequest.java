package com.shoppy.backend.domain.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không quá 100 ký tự")
    private String name;

    @Size(max = 100, message = "Slug không quá 100 ký tự")
    private String slug;

    private String description;

    private Long parentId;

    @Builder.Default
    private Integer displayOrder = 0;

    @Builder.Default
    private String status = "ACTIVE";
}
