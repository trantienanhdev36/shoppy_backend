package com.shoppy.backend.domain.request.admin;

import com.shoppy.backend.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái người dùng không được để trống")
    private UserStatus status;

    @Size(max = 255, message = "Lý do thay đổi trạng thái không được vượt quá 255 ký tự")
    private String reason;
}
