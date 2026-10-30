package com.shoppy.backend.domain.request.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryStatusRequest {

    @NotBlank(message = "Trạng thái danh mục không được để trống")
    private String status; // ACTIVE, INACTIVE
}
