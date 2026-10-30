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
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private Integer totalStock;
    private boolean hasVariants;
    private Boolean isActive;
    private List<ProductImageDTO> images; // Ảnh sản phẩm gốc (nếu không có biến thể)
    private List<ProductVariantDTO> variants; // Danh sách biến thể (nếu hasVariants = true)

    public static ProductDetailResponseDTO fromEntity(Product product) {
        if (product == null) return null;

        List<ProductVariantDTO> variantDTOs = null;
        List<ProductImageDTO> imageDTOs = null;
        Integer totalStock = null;

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            totalStock = product.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            variantDTOs = product.getVariants().stream()
                    .map(ProductVariantDTO::fromEntity)
                    .toList();
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageDTOs = product.getImages().stream()
                    .map(ProductImageDTO::fromEntity)
                    .toList();
        }

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        Long shopId = product.getShop() != null ? product.getShop().getId() : null;
        String shopName = product.getShop() != null ? product.getShop().getName() : null;

        return ProductDetailResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .shopId(shopId)
                .shopName(shopName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .totalStock(totalStock)
                .hasVariants(product.isHasVariants())
                .isActive(product.getIsActive())
                .images(imageDTOs)
                .variants(variantDTOs)
                .build();
    }
}