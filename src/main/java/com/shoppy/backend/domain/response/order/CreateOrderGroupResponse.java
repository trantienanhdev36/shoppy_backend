package com.shoppy.backend.domain.response.order;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import com.shoppy.backend.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response trả về khi khách hàng đặt đơn hàng.
 * Hỗ trợ cả 2 trường hợp:
 *  - Đặt từ 1 shop  → orders có 1 phần tử, orderGroupId = null
 *  - Đặt từ N shop  → orders có N phần tử, orderGroupId = "GRP-YYYYMMDD-XXXXXXXX"
 */
@Data
@Builder
public class CreateOrderGroupResponse {

    /** Mã nhóm đơn hàng. null nếu chỉ có 1 shop. */
    private String orderGroupId;

    /** Tổng số đơn hàng được tạo (= số shop khác nhau trong request). */
    private int totalOrders;

    /** Tổng tiền hàng (chưa gồm phí ship) của tất cả đơn trong nhóm. */
    private BigDecimal totalAmount;

    /** Tổng phí ship của tất cả đơn trong nhóm. */
    private BigDecimal totalShippingFee;

    /** Tổng thanh toán (tiền hàng + phí ship) của tất cả đơn trong nhóm. */
    private BigDecimal grandTotal;

    /** Danh sách đơn hàng được tạo. */
    private List<OrderSummary> orders;

    // ── Inner DTO: thông tin tóm tắt mỗi đơn trong nhóm ─────────────────────
    @Data
    @Builder
    public static class OrderSummary {
        private Long orderId;
        private Long shopId;
        private OrderStatus status;
        private PaymentMethod paymentMethod;
        private BigDecimal totalAmount;
        private BigDecimal shippingFee;
        private BigDecimal grandTotal;
        private int itemCount;
        private LocalDateTime createdAt;

        public static OrderSummary fromEntity(Order order, int itemCount) {
            return OrderSummary.builder()
                    .orderId(order.getId())
                    .shopId(order.getShopId())
                    .status(order.getStatus())
                    .paymentMethod(order.getPaymentMethod())
                    .totalAmount(order.getTotalAmount())
                    .shippingFee(order.getShippingFee())
                    .grandTotal(order.getTotalAmount().add(order.getShippingFee()))
                    .itemCount(itemCount)
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }
}

