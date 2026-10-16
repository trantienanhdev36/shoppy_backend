package com.shoppy.backend.common.enums;

public enum TrackingStatus {
    ORDER_PLACED,       // Đơn hàng đã đặt
    PAYMENT_CONFIRMED,  // Thanh toán xác nhận
    PREPARING,          // Shop đang chuẩn bị
    READY_FOR_PICKUP,   // Sẵn sàng để shipper lấy
    PICKED_UP,          // Shipper đã lấy hàng
    IN_TRANSIT,         // Đang vận chuyển
    DELIVERED,          // Giao thành công
    CANCELLED           // Đã hủy
}
