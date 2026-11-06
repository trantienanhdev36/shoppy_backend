package com.shoppy.backend.domain.response.shop;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.entity.Shop;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ShopPublicProfileResponse(
        Long id,
        String name,
        String description,
        String logoUrl,
        String address,
        String status,
        LocalDateTime createdAt,
        PagedResponse<ProductResponseDTO> products
) {
    public static ShopPublicProfileResponse fromEntity(Shop shop, PagedResponse<ProductResponseDTO> products) {
        if (shop == null) return null;
        return ShopPublicProfileResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .products(products)
                .build();
    }
}
