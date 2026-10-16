package com.shoppy.backend.common.enums;

public enum OrderStatus {
    PENDING,        // Chờ xử lý (vừa tạo, chưa kiểm tra kho/thanh toán)
    PROCESSING,     // Đang xử lý (đã xác nhận kho + thanh toán, shop chuẩn bị)
    READY_TO_SHIP,  // Sẵn sàng giao hàng (shop đóng gói xong)
    SHIPPING,       // Đang giao hàng (shipper đã nhận đơn)
    DELIVERED,      // Đã giao thành công
    CANCELLED       // Đã hủy (thiếu kho, thiếu tiền, hoặc do yêu cầu)
}
