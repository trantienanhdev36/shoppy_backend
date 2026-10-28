package com.shoppy.backend.domain.request.shipper;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterShipperRequest {

    @NotNull(message = "Công ty vận chuyển không được để trống")
    private Long shippingCompanyId;
}
