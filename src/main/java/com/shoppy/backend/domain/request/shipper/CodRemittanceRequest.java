package com.shoppy.backend.domain.request.shipper;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodRemittanceRequest {

    @NotNull(message = "Số tiền nộp không được để trống")
    @DecimalMin(value = "1000", message = "Số tiền nộp tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private String note;
}
