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
    EXPIRED_TOKEN(4006, "Đường link xác thực đã hết hạn", HttpStatus.BAD_REQUEST),
    NOTIFICATION_NOT_FOUND(4010, "Thông báo không tồn tại", HttpStatus.NOT_FOUND),
    NOTIFICATION_ACCESS_DENIED(4011, "Bạn không có quyền thao tác trên thông báo này", HttpStatus.FORBIDDEN),
    ORDER_NOT_FOUND(4012, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_WRONG_STATUS(4013, "Đơn hàng không ở trạng thái phù hợp để thực hiện thao tác này", HttpStatus.BAD_REQUEST),
    SHIPPER_WRONG_COMPANY(4014, "Shipper không thuộc hãng vận chuyển của đơn hàng này", HttpStatus.FORBIDDEN),
    SHIPPER_NOT_ASSIGNED(4015, "Bạn không phải shipper được giao đơn hàng này", HttpStatus.FORBIDDEN),
    SHIPPING_COMPANY_REQUIRED(4016, "Shipper chưa được gán cho hãng vận chuyển nào", HttpStatus.BAD_REQUEST),
    ALREADY_A_SHIPPER(4017, "Tài khoản của bạn đã là nhân viên giao hàng", HttpStatus.BAD_REQUEST),
    ONLY_CUSTOMER_CAN_REGISTER_SHIPPER(4018, "Chỉ tài khoản khách hàng mới có thể đăng ký làm Shipper", HttpStatus.BAD_REQUEST),
    SHIPPING_COMPANY_NOT_ACTIVE(4019, "Hãng vận chuyển hiện không hoạt động", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}