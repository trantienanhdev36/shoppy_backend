package com.shoppy.backend.domain.response.product;

import com.shoppy.backend.entity.Product;
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
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private boolean hasVariants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;

    public static ProductResponseDTO fromEntity(Product product) {
        return fromEntity(product, null, null);
    }

    public static ProductResponseDTO fromEntity(Product product, BigDecimal minPrice, BigDecimal maxPrice) {
        if (product == null) return null;

        BigDecimal min = minPrice;
        BigDecimal max = maxPrice;

        // Nếu min/max chưa được cung cấp, tự tính từ variants nếu có
        if (min == null && max == null && product.getVariants() != null && !product.getVariants().isEmpty()) {
            min = product.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            max = product.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
        }

        List<ProductImageDTO> imageDTOs = null;
        if (product.getImages() != null) {
            imageDTOs = product.getImages().stream()
                    .map(ProductImageDTO::fromEntity)
                    .toList();
        }

        List<ProductVariantDTO> variantDTOs = null;
        if (product.getVariants() != null) {
            variantDTOs = product.getVariants().stream()
                    .map(ProductVariantDTO::fromEntity)
                    .toList();
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariants(product.isHasVariants())
                .minPrice(min)
                .maxPrice(max)
                .variants(variantDTOs)
                .images(imageDTOs)
                .build();
    }
}