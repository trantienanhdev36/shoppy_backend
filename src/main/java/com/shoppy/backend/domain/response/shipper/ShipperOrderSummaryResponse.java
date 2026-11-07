package com.shoppy.backend.domain.response.shipper;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipperOrderSummaryResponse {
    private Long id;
    private String shopAddress;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private String shippingAddress;
    private String trackingNumber;
    private int itemCount;
    private LocalDateTime createdAt;

    public static ShipperOrderSummaryResponse fromEntity(Order order, String shopAddress) {
        return ShipperOrderSummaryResponse.builder()
                .id(order.getId())
                .shopAddress(shopAddress)
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getTotalAmount().add(order.getShippingFee()))
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
