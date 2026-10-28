package com.shoppy.backend.domain.response.shipper;

import com.shoppy.backend.common.enums.ShipperTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodBalanceResponse {
    private BigDecimal totalCollected;   // Tổng tiền mặt đã thu từ khách (COLLECTION)
    private BigDecimal totalRemitted;    // Tổng tiền đã nộp về công ty (REMITTANCE)
    private BigDecimal currentBalance;   // Số dư đang giữ = totalCollected - totalRemitted
}
