package com.shoppy.backend.domain.response.product;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private boolean hasVariants;
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;
}