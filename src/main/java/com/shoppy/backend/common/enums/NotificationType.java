package com.shoppy.backend.common.enums;

public enum NotificationType {
    ORDER_PLACED,         // Customer: Đặt hàng thành công
    ORDER_STATUS_CHANGED, // Customer: Trạng thái đơn hàng thay đổi
    NEW_ORDER_FOR_SELLER, // Seller: Có đơn hàng mới tại shop
    WALLET_DEPOSIT,       // Customer: Nạp tiền vào ví thành công
    EMAIL_VERIFIED        // Customer: Kích hoạt email thành công
}
