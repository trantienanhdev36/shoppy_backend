package com.shoppy.backend.domain.request.order;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {

    @Size(max = 255, message = "Lý do hủy đơn không được vượt quá 255 ký tự")
    private String reason;
}
