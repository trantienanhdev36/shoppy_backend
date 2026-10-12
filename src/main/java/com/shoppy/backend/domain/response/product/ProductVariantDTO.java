package com.shoppy.backend.domain.response.product;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal; // <-- Import BigDecimal
import java.util.List;

@Getter
@Setter
public class ProductVariantDTO {
    private Long id;
    private String sku;
    private BigDecimal price; // <-- Khớp với kiểu dữ liệu giá tiền trong Entity
    private int stockQuantity;
    private List<ProductImageDTO> images; // Ảnh riêng của biến thể (nếu có)
}