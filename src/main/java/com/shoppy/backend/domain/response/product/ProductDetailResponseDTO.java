package com.shoppy.backend.domain.response.product;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ProductDetailResponseDTO {
    private Long id;
    private String name;
    private String description;
    private boolean hasVariants;
    private List<ProductImageDTO> images; // Ảnh sản phẩm gốc (nếu không có biến thể)
    private List<ProductVariantDTO> variants; // Danh sách biến thể (nếu hasVariants = true)
}