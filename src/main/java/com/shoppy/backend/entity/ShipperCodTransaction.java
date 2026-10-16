package com.shoppy.backend.entity;

import com.shoppy.backend.common.enums.ShipperTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Bản ghi giao dịch COD của shipper.
 * Khi shipper giao hàng thành công (COD), hệ thống tạo một bản ghi COLLECTION
 * để theo dõi số tiền mặt shipper đang giữ, phục vụ đối soát tài chính.
 */
@Entity
@Table(name = "shipper_cod_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperCodTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "shipper_id", nullable = false)
    private Long shipperId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ShipperTransactionType type;

    // Tổng tiền COD (totalAmount + shippingFee)
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
