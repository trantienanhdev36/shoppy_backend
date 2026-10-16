package com.shoppy.backend.entity;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.common.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho một đơn hàng.
 * Sau khi tạo, đơn hàng sẽ được đẩy vào Redis Queue để Worker xử lý ngầm.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Thông tin người đặt hàng ──────────────────────────────────────────────
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    // Cột user_id (schema cũ) - giữ cùng giá trị với customerId để tương thích DB
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ── Thông tin hãng vận chuyển ─────────────────────────────────────────────
    @Column(name = "shipping_company_id", nullable = false)
    private Long shippingCompanyId;

    // ── Shop xử lý đơn hàng ──────────────────────────────────────────────────
    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    // ── Phương thức thanh toán ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    // ── Trạng thái đơn hàng ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // ── Thông tin giao hàng ───────────────────────────────────────────────────
    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    // ── Tài chính ─────────────────────────────────────────────────────────────
    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;   // Map sang cột total_price trong DB

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    // ── Ghi chú ───────────────────────────────────────────────────────────────
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ── Shipper được assign ───────────────────────────────────────────────────
    @Column(name = "shipper_id")
    private Long shipperId;

    // ── Mã vận đơn (do hãng ship cấp sau khi lấy hàng) ───────────────────────
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    // ── Chi tiết sản phẩm ─────────────────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ── Lịch sử tracking ──────────────────────────────────────────────────────
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderTracking> trackings = new ArrayList<>();
}
