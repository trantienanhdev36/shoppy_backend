package com.shoppy.backend.domain.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminShopDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    private String googleMapUrl;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin chủ Shop
    private OwnerInfo owner;

    // Thống kê
    private Long totalProductsCount;
    private Long totalOrdersCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfo {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String status;
    }

    public static AdminShopDetailResponse fromEntity(Shop shop, User owner, long totalProducts, long totalOrders) {
        if (shop == null) {
            return null;
        }

        OwnerInfo ownerInfo = null;
        if (owner != null) {
            ownerInfo = OwnerInfo.builder()
                    .id(owner.getId())
                    .email(owner.getEmail())
                    .fullName(owner.getFullName())
                    .phone(owner.getPhone())
                    .status(owner.getStatus() != null ? owner.getStatus().name() : null)
                    .build();
        }

        return AdminShopDetailResponse.builder()
                .id(shop.getId())
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .googleMapUrl(shop.getGoogleMapUrl())
                .balance(shop.getBalance())
                .status(shop.getStatus())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .owner(ownerInfo)
                .totalProductsCount(totalProducts)
                .totalOrdersCount(totalOrders)
                .build();
    }
}
