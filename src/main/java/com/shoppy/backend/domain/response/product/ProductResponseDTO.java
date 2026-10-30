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
    private String thumbnailUrl;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String categoryName;
    private Integer totalStock;
    private boolean hasVariants;
    private Boolean isActive;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
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
        String thumbnailUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageDTOs = product.getImages().stream()
                    .map(ProductImageDTO::fromEntity)
                    .toList();
            thumbnailUrl = product.getImages().get(0).getImageUrl();
        }

        // Nếu chưa có ảnh từ danh sách ảnh sản phẩm, lấy ảnh từ biến thể đầu tiên
        if (thumbnailUrl == null && product.getVariants() != null && !product.getVariants().isEmpty()) {
            thumbnailUrl = product.getVariants().stream()
                    .map(ProductVariant::getImageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        }

        Integer totalStock = null;
        if (product.getVariants() != null) {
            totalStock = product.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
        }

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        Long shopId = product.getShop() != null ? product.getShop().getId() : null;
        String shopName = product.getShop() != null ? product.getShop().getName() : null;

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .shopId(shopId)
                .shopName(shopName)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .totalStock(totalStock)
                .hasVariants(product.isHasVariants())
                .isActive(product.getIsActive())
                .minPrice(min)
                .maxPrice(max)
                .images(imageDTOs)
                .build();
    }
}