package com.shoppy.backend.domain.response.user;

import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.entity.WalletDeposit;
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
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private UserStatus status;

    @Builder
    public static record WalletDepositResponse(
            Long id,
            Long userId,
            BigDecimal amount,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        // Hàm tĩnh dùng để chuyển đổi nhanh từ Entity -> Response DTO
        public static WalletDepositResponse fromEntity(WalletDeposit entity) {
            if (entity == null) {
                return null;
            }
            return WalletDepositResponse.builder()
                    .id(entity.getId())
                    .userId(entity.getUserId())
                    .amount(entity.getAmount())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}