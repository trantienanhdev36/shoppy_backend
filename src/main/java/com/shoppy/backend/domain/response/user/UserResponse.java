package com.shoppy.backend.domain.response.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.entity.User;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private UserStatus status;
    private BigDecimal balance;
    private String avatarUrl;
    private Long shippingCompanyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Chuyển đổi từ User Entity sang UserResponse DTO
     */
    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus())
                .balance(user.getBalance())
                .avatarUrl(user.getAvatarUrl())
                .shippingCompanyId(user.getShippingCompanyId())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Builder
    public static record WalletDepositResponse(
            Long id,
            Long userId,
            BigDecimal amount,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        public static WalletDepositResponse fromEntity(WalletTransaction entity) {
            if (entity == null) {
                return null;
            }
            return WalletDepositResponse.builder()
                    .id(entity.getLegacyDepositId() != null
                            ? entity.getLegacyDepositId()
                            : entity.getId())
                    .userId(entity.getUserId())
                    .amount(entity.getAmount())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
}