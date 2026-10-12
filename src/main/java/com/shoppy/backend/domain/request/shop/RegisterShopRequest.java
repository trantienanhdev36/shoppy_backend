package com.shoppy.backend.domain.request.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class RegisterShopRequest {

    @NotBlank(message = "Tên Shop không được để trống")
    @Size(max = 255, message = "Tên Shop không vượt quá 255 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả shop không được vượt quá 1000 ký tự")
    private String description;

    // Tùy chọn: Thêm ràng buộc kiểm tra sơ bộ định dạng URL Google Maps nếu cần
    @Size(max = 500, message = "Đường dẫn Google Map tối đa 500 ký tự")
    private String googleMapUrl;
}