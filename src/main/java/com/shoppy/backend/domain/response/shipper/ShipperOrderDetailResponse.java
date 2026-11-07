package com.shoppy.backend.domain.response.shipper;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.domain.response.order.OrderResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShipperOrderDetailResponse {
    private Long id;
    private Long customerId;
    private String shopAddress;
    private Long shippingCompanyId;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private String note;
    private Long shipperId;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderResponse.OrderItemResponse> items;
    private List<OrderResponse.OrderTrackingResponse> trackings;

    public static ShipperOrderDetailResponse fromEntity(
            OrderResponse.OrderDetailResponse order, String shopAddress) {
        return ShipperOrderDetailResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .shopAddress(shopAddress)
                .shippingCompanyId(order.getShippingCompanyId())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getGrandTotal())
                .note(order.getNote())
                .shipperId(order.getShipperId())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems())
                .trackings(order.getTrackings())
                .build();
    }
}
