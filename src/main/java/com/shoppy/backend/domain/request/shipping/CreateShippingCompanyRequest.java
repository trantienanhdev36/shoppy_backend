package com.shoppy.backend.domain.request.shipping;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShippingCompanyRequest {

    @NotBlank(message = "Tên đơn vị vận chuyển không được để trống")
    @Size(max = 100, message = "Tên đơn vị vận chuyển không quá 100 ký tự")
    private String name;

    @NotBlank(message = "Mã đơn vị vận chuyển không được để trống")
    @Size(max = 50, message = "Mã đơn vị vận chuyển không quá 50 ký tự")
    private String code;

    @NotNull(message = "Cước phí cơ bản không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cước phí cơ bản phải lớn hơn hoặc bằng 0")
    @Builder.Default
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Builder.Default
    private String status = "ACTIVE";
}
