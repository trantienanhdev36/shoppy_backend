package com.shoppy.backend.domain.request.product;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}
