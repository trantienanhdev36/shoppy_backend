package com.shoppy.backend.domain.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.entity.ShippingCompany;
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
public class AdminUserDetailResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private BigDecimal balance;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Chi tiết bổ sung
    private Long totalOrdersCount;
    private ShopSummary shop;
    private ShippingCompanySummary shippingCompany;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopSummary {
        private Long id;
        private String name;
        private String status;
        private BigDecimal balance;
        private String logoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingCompanySummary {
        private Long id;
        private String name;
        private String code;
        private String status;
    }

    public static AdminUserDetailResponse fromEntity(User user, Shop shop, long totalOrdersCount) {
        if (user == null) {
            return null;
        }

        ShopSummary shopSummary = null;
        if (shop != null) {
            shopSummary = ShopSummary.builder()
                    .id(shop.getId())
                    .name(shop.getName())
                    .status(shop.getStatus())
                    .balance(shop.getBalance())
                    .logoUrl(shop.getLogoUrl())
                    .build();
        }

        ShippingCompanySummary shippingCompanySummary = null;
        ShippingCompany company = user.getShippingCompany();
        if (company != null) {
            shippingCompanySummary = ShippingCompanySummary.builder()
                    .id(company.getId())
                    .name(company.getName())
                    .code(company.getCode())
                    .status(company.getStatus())
                    .build();
        }

        return AdminUserDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .balance(user.getBalance())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalOrdersCount(totalOrdersCount)
                .shop(shopSummary)
                .shippingCompany(shippingCompanySummary)
                .build();
    }
}
