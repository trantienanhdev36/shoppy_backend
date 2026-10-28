package com.shoppy.backend.domain.response.shipper;

import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.entity.ShippingCompany;
import com.shoppy.backend.entity.User;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ShipperResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        UserRole role,
        Long shippingCompanyId,
        String shippingCompanyName,
        String shippingCompanyLogo,
        LocalDateTime updatedAt
) {
    public static ShipperResponse fromEntity(User user) {
        if (user == null) return null;
        ShippingCompany company = user.getShippingCompany();
        return ShipperResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .shippingCompanyId(company != null ? company.getId() : null)
                .shippingCompanyName(company != null ? company.getName() : null)
                .shippingCompanyLogo(company != null ? company.getLogoUrl() : null)
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
