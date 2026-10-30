package com.shoppy.backend.domain.response.wallet;

import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.WalletTransactionType;
import com.shoppy.backend.entity.WalletTransaction;
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
public class WalletTransactionResponse {

    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private WalletTransactionType type;
    private TransactionStatus status;
    private LocalDateTime createdAt;

    public static WalletTransactionResponse fromEntity(WalletTransaction entity) {
        if (entity == null) {
            return null;
        }
        return WalletTransactionResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .type(entity.getType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
