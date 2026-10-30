package com.shoppy.backend.domain.request.shipping;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShippingCompanyStatusRequest {

    @NotBlank(message = "Trạng thái đơn vị vận chuyển không được để trống (phải là ACTIVE hoặc INACTIVE)")
    private String status;
}
