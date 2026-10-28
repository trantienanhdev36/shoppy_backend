package com.shoppy.backend.domain.response.product;

import com.shoppy.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponseDTO {
    private Long id;
    private String name;
    private String description;
    private boolean hasVariants;
    private List<ProductImageDTO> images; // Ảnh sản phẩm gốc (nếu không có biến thể)
    private List<ProductVariantDTO> variants; // Danh sách biến thể (nếu hasVariants = true)

    public static ProductDetailResponseDTO fromEntity(Product product) {
        if (product == null) return null;

        List<ProductVariantDTO> variantDTOs = null;
        List<ProductImageDTO> imageDTOs = null;

        if (product.isHasVariants() && product.getVariants() != null && !product.getVariants().isEmpty()) {
            variantDTOs = product.getVariants().stream()
                    .map(ProductVariantDTO::fromEntity)
                    .toList();
        } else if (product.getImages() != null) {
            imageDTOs = product.getImages().stream()
                    .map(ProductImageDTO::fromEntity)
                    .toList();
        }

        return ProductDetailResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariants(product.isHasVariants())
                .images(imageDTOs)
                .variants(variantDTOs)
                .build();
    }
}