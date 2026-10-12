package com.shoppy.backend.domain.response.shop;

import com.shoppy.backend.entity.Shop;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ShopResponse(
        Long id,
        Long userId,
        String name,
        String description,
        String logoUrl,
        String googleMapUrl,
        BigDecimal balance,
        String status,
        LocalDateTime createdAt
) {
    public static ShopResponse fromEntity(Shop shop) {
        if (shop == null) return null;
        return ShopResponse.builder()
                .id(shop.getId())
                .userId(shop.getUserId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .googleMapUrl(shop.getGoogleMapUrl())
                .balance(shop.getBalance())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .build();
    }
}