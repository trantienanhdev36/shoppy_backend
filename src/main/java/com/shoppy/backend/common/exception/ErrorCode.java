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
    SHIPPING_COMPANY_NOT_ACTIVE(4019, "Hãng vận chuyển hiện không hoạt động", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND(4020, "Bạn chưa đăng ký hoặc sở hữu cửa hàng nào", HttpStatus.NOT_FOUND),
    ORDER_NOT_BELONG_TO_SHOP(4021, "Đơn hàng không thuộc quyền quản lý của Cửa hàng bạn", HttpStatus.FORBIDDEN),
    CATEGORY_NOT_FOUND(4022, "Danh mục sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(4023, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_BELONG_TO_SHOP(4024, "Sản phẩm không thuộc quyền quản lý của Cửa hàng bạn", HttpStatus.FORBIDDEN),
    PRODUCT_VARIANT_NOT_FOUND(4025, "Không tìm thấy biến thể sản phẩm", HttpStatus.NOT_FOUND),
    SHOP_NAME_EXISTED(4026, "Tên Cửa hàng đã được sử dụng, vui lòng chọn tên khác", HttpStatus.BAD_REQUEST),
    SHOP_NOT_ACTIVE(4027, "Cửa hàng hiện đang tạm ngưng hoạt động", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_LAST_VARIANT(4028, "Không thể xóa biến thể duy nhất của sản phẩm. Bạn có thể tạm ẩn hoặc xóa sản phẩm.", HttpStatus.BAD_REQUEST),
    INVALID_OTP(4029, "Mã OTP không chính xác", HttpStatus.BAD_REQUEST),
    EXPIRED_OTP(4030, "Mã OTP đã hết hạn hoặc chưa được gửi", HttpStatus.BAD_REQUEST),
    OTP_COOLDOWN(4031, "Vui lòng đợi 60 giây trước khi yêu cầu gửi lại mã OTP", HttpStatus.TOO_MANY_REQUESTS),
    ORDER_NOT_BELONG_TO_CUSTOMER(4032, "Bạn không có quyền thao tác trên đơn hàng của người khác", HttpStatus.FORBIDDEN),
    ORDER_ALREADY_PREPARED(4033, "Người bán đã chuẩn bị xong đơn hàng hoặc đang giao, không thể hủy", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_CANCELLED(4034, "Đơn hàng này đã bị hủy trước đó", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_SELF(4035, "Bạn không thể tự khóa hoặc vô hiệu hóa tài khoản của chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_CHANGE_SELF_ROLE(4036, "Bạn không thể tự thay đổi vai trò của chính mình", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_ADMIN(4037, "Không thể khóa hoặc vô hiệu hóa tài khoản quản trị viên", HttpStatus.BAD_REQUEST),
    CANNOT_DEMOTE_ADMIN(4038, "Không thể tước hoặc hạ vai trò của tài khoản quản trị viên", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_EXISTED(4039, "Tên danh mục đã tồn tại, vui lòng chọn tên khác", HttpStatus.BAD_REQUEST),
    CATEGORY_SLUG_EXISTED(4040, "Đường dẫn slug của danh mục đã tồn tại", HttpStatus.BAD_REQUEST),
    CATEGORY_PARENT_INVALID(4041, "Danh mục cha không hợp lệ (không thể chọn chính mình hoặc danh mục con làm cha)", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_CATEGORY_HAS_PRODUCTS(4042, "Không thể xóa danh mục đang có sản phẩm kinh doanh", HttpStatus.BAD_REQUEST),
    INVALID_SHOP_STATUS(4043, "Trạng thái Cửa hàng không hợp lệ (phải là ACTIVE, INACTIVE hoặc BLOCKED)", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_ACTIVE(4044, "Danh mục sản phẩm hiện đang tạm ngưng hoạt động", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_STATUS(4045, "Trạng thái danh mục không hợp lệ (phải là ACTIVE hoặc INACTIVE)", HttpStatus.BAD_REQUEST),
    SHIPPING_COMPANY_CODE_EXISTED(4046, "Mã đơn vị vận chuyển đã tồn tại, vui lòng chọn mã khác", HttpStatus.BAD_REQUEST),
    SHIPPING_COMPANY_NAME_EXISTED(4047, "Tên đơn vị vận chuyển đã tồn tại, vui lòng chọn tên khác", HttpStatus.BAD_REQUEST),
    INVALID_SHIPPING_COMPANY_STATUS(4048, "Trạng thái đơn vị vận chuyển không hợp lệ (phải là ACTIVE hoặc INACTIVE)", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}