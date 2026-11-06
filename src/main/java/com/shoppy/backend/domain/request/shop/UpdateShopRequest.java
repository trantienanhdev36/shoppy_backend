package com.shoppy.backend.domain.request.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShopRequest {

    @NotBlank(message = "Tên Cửa hàng không được để trống")
    @Size(max = 100, message = "Tên Cửa hàng tối đa 100 ký tự")
    private String name;

    private String description;

    @Size(max = 500, message = "Địa chỉ shop tối đa 500 ký tự")
    private String address;
}
