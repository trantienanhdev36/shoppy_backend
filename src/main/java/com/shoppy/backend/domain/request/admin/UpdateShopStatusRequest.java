package com.shoppy.backend.domain.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShopStatusRequest {

    @NotBlank(message = "Trạng thái Cửa hàng không được để trống")
    private String status; // ACTIVE, INACTIVE, BLOCKED

    @Size(max = 255, message = "Lý do thay đổi trạng thái không quá 255 ký tự")
    private String reason;
}
