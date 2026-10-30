package com.shoppy.backend.domain.response.product;

import com.shoppy.backend.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private String variantName;
    private String sku;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private Boolean isActive;
    private List<ProductImageDTO> images;

    public static ProductVariantDTO fromEntity(ProductVariant variant) {
        if (variant == null) return null;
        List<ProductImageDTO> images = null;
        if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) {
            images = List.of(ProductImageDTO.builder()
                    .imageUrl(variant.getImageUrl())
                    .build());
        }
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(variant.getImageUrl())
                .isActive(variant.getIsActive() != null ? variant.getIsActive() : true)
                .images(images)
                .build();
    }
}