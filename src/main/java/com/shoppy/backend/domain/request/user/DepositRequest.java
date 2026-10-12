package com.shoppy.backend.domain.request.user;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền nạp tối thiểu là 10,000 VNĐ")
    private BigDecimal amount;
}