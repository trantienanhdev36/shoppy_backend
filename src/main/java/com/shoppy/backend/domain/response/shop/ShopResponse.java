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
        String address,
        BigDecimal balance,
        String status,
        long totalProducts,
        long totalOrders,
        LocalDateTime createdAt
) {
    /** Factory method dùng khi KHÔNG có số liệu thống kê (ví dụ: sau khi đăng ký shop). */
    public static ShopResponse fromEntity(Shop shop) {
        if (shop == null) return null;
        return ShopResponse.builder()
                .id(shop.getId())
                .userId(shop.getUserId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .balance(shop.getBalance())
                .status(shop.getStatus())
                .totalProducts(0L)
                .totalOrders(0L)
                .createdAt(shop.getCreatedAt())
                .build();
    }

    /** Factory method dùng khi CÓ số liệu thống kê (GET /shops/me). */
    public static ShopResponse fromEntity(Shop shop, long totalProducts, long totalOrders) {
        if (shop == null) return null;
        return ShopResponse.builder()
                .id(shop.getId())
                .userId(shop.getUserId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .address(shop.getAddress())
                .balance(shop.getBalance())
                .status(shop.getStatus())
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .createdAt(shop.getCreatedAt())
                .build();
    }
}