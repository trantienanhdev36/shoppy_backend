package com.shoppy.backend.domain.response.order;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.common.enums.TrackingStatus;
import com.shoppy.backend.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {

    // ── Response cho tạo đơn hàng (trả về ngay sau khi push queue) ───────────
    @Data
    @Builder
    public static class CreateOrderResponse {
        private Long orderId;
        private OrderStatus status;
        private String message;
        private LocalDateTime createdAt;

        public static CreateOrderResponse fromEntity(Order order) {
            return CreateOrderResponse.builder()
                    .orderId(order.getId())
                    .status(order.getStatus())
                    .message("Đơn hàng đã được tiếp nhận và đang được xử lý")
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }

    // ── Response chi tiết đơn hàng ────────────────────────────────────────────
    @Data
    @Builder
    public static class OrderDetailResponse {
        private Long id;
        private Long customerId;
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
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OrderItemResponse> items;
        private List<OrderTrackingResponse> trackings;

        public static OrderDetailResponse fromEntity(Order order) {
            return OrderDetailResponse.builder()
                    .id(order.getId())
                    .customerId(order.getCustomerId())
                    .shippingCompanyId(order.getShippingCompanyId())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .shippingAddress(order.getShippingAddress())
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .totalAmount(order.getTotalAmount())
                    .shippingFee(order.getShippingFee())
                    .grandTotal(order.getTotalAmount().add(order.getShippingFee()))
                    .note(order.getNote())
                    .shipperId(order.getShipperId())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .items(order.getItems().stream()
                            .map(OrderItemResponse::fromEntity)
                            .collect(Collectors.toList()))
                    .trackings(order.getTrackings().stream()
                            .map(OrderTrackingResponse::fromEntity)
                            .collect(Collectors.toList()))
                    .build();
        }
    }

    // ── Response tóm tắt đơn hàng (dùng trong danh sách) ─────────────────────
    @Data
    @Builder
    public static class OrderSummaryResponse {
        private Long id;
        private OrderStatus status;
        private PaymentMethod paymentMethod;
        private BigDecimal totalAmount;
        private BigDecimal shippingFee;
        private BigDecimal grandTotal;
        private String shippingAddress;
        private int itemCount;
        private LocalDateTime createdAt;

        public static OrderSummaryResponse fromEntity(Order order) {
            return OrderSummaryResponse.builder()
                    .id(order.getId())
                    .status(order.getStatus())
                    .paymentMethod(order.getPaymentMethod())
                    .totalAmount(order.getTotalAmount())
                    .shippingFee(order.getShippingFee())
                    .grandTotal(order.getTotalAmount().add(order.getShippingFee()))
                    .shippingAddress(order.getShippingAddress())
                    .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }

    // ── Response chi tiết OrderItem ───────────────────────────────────────────
    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private Long productVariantId;
        private String productNameSnapshot;
        private String variantNameSnapshot;
        private BigDecimal priceSnapshot;
        private Integer quantity;
        private BigDecimal subtotal;

        public static OrderItemResponse fromEntity(OrderItem item) {
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productVariantId(item.getProductVariantId())
                    .productNameSnapshot(item.getProductNameSnapshot())
                    .variantNameSnapshot(item.getVariantNameSnapshot())
                    .priceSnapshot(item.getPriceSnapshot())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .build();
        }
    }

    // ── Response Tracking ─────────────────────────────────────────────────────
    @Data
    @Builder
    public static class OrderTrackingResponse {
        private Long id;
        private TrackingStatus status;
        private String description;
        private Long updatedBy;
        private LocalDateTime createdAt;

        public static OrderTrackingResponse fromEntity(OrderTracking tracking) {
            return OrderTrackingResponse.builder()
                    .id(tracking.getId())
                    .status(tracking.getStatus())
                    .description(tracking.getDescription())
                    .updatedBy(tracking.getUpdatedBy())
                    .createdAt(tracking.getCreatedAt())
                    .build();
        }
    }

    // ── Response ShipperCodTransaction ────────────────────────────────────────
    @Data
    @Builder
    public static class ShipperCodTransactionResponse {
        private Long id;
        private Long orderId;
        private Long shipperId;
        private ShipperTransactionType type;
        private BigDecimal amount;
        private String note;
        private LocalDateTime createdAt;

        public static ShipperCodTransactionResponse fromEntity(ShipperCodTransaction tx) {
            return ShipperCodTransactionResponse.builder()
                    .id(tx.getId())
                    .orderId(tx.getOrderId())
                    .shipperId(tx.getShipperId())
                    .type(tx.getType())
                    .amount(tx.getAmount())
                    .note(tx.getNote())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }
    }
}
