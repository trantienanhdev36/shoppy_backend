package com.shoppy.backend.domain.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddProductVariantRequest {

    @NotBlank(message = "Tên biến thể không được để trống")
    private String variantName;

    @NotNull(message = "Giá biến thể không được để trống")
    @Min(value = 0, message = "Giá biến thể phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stockQuantity;

    private String sku;

    private String imageUrl;
}
