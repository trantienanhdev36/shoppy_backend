package com.shoppy.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(4007, "Tài khoản không tồn tại trên hệ thống", HttpStatus.NOT_FOUND),
    WRONG_PASSWORD(4008, "Mật khẩu không chính xác", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_ACTIVATED(4009, "Tài khoản chưa được kích hoạt email", HttpStatus.FORBIDDEN),
    EMAIL_EXISTED(4001, "Email này đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(4002, "Số điện thoại này đã được sử dụng", HttpStatus.BAD_REQUEST),
    SHIPPER_COMPANY_REQUIRED(4003, "Tài khoản Shipper phải chọn Hãng vận chuyển", HttpStatus.BAD_REQUEST),
    SHIPPING_COMPANY_NOT_FOUND(4004, "Không tìm thấy Hãng vận chuyển tương ứng", HttpStatus.NOT_FOUND),
    INVALID_TOKEN(4005, "Token xác thực không hợp lệ hoặc không tồn tại", HttpStatus.BAD_REQUEST),
    EXPIRED_TOKEN(4006, "Đường link xác thực đã hết hạn", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}