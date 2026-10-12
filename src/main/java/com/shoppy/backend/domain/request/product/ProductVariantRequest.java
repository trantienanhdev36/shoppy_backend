package com.shoppy.backend.domain.request.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {
    private String variantName; // Ví dụ: "Đỏ - Size L" (Nếu không có biến thể, có thể bỏ trống hoặc truyền "Mặc định")

    @NotNull(message = "Giá biến thể không được để trống")
    @Min(value = 0, message = "Giá phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stockQuantity;

    private String sku;
}