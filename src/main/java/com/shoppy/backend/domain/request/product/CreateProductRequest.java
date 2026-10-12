package com.shoppy.backend.domain.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    private String description;

    // Dùng cho trường hợp sản phẩm KHÔNG CÓ BIẾN THỂ (Sản phẩm đơn)
    private BigDecimal basePrice;
    private Integer baseStock;

    // Danh sách biến thể (Nếu sản phẩm có phân loại hàng)
    @Valid
    private List<ProductVariantRequest> variants= new ArrayList<>();
}