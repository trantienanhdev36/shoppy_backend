package com.shoppy.backend.domain.request.order;

import com.shoppy.backend.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Request body để khách hàng tạo đơn hàng mới.
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "Vui lòng chọn hãng vận chuyển")
    private Long shippingCompanyId;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
    private String shippingAddress;

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    private String recipientName;

    @NotBlank(message = "Vui lòng nhập số điện thoại người nhận")
    private String recipientPhone;

    private String note;

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotNull(message = "productVariantId không được để trống")
        private Long productVariantId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer quantity;
    }
}
