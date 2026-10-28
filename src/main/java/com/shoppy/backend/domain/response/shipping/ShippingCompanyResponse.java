package com.shoppy.backend.domain.response.shipping;

import com.shoppy.backend.entity.ShippingCompany;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ShippingCompanyResponse(
        Long id,
        String name,
        String code,
        String logoUrl,
        BigDecimal baseFee,
        String status
) {
    public static ShippingCompanyResponse fromEntity(ShippingCompany company) {
        if (company == null) return null;
        return ShippingCompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .logoUrl(company.getLogoUrl())
                .baseFee(company.getBaseFee())
                .status(company.getStatus())
                .build();
    }
}
