package com.shoppy.backend.domain.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateProductVariantRequest {

    /**
     * ID của biến thể nếu là cập nhật biến thể cũ.
     * Để trống (null) nếu là biến thể mới thêm vào.
     */
    private Long id;

    private String variantName;

    @NotNull(message = "Giá biến thể không được để trống")
    @Min(value = 0, message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stockQuantity;

    private String sku;

    /**
     * URL ảnh cũ của biến thể nếu không thay đổi ảnh.
     */
    private String imageUrl;
}
