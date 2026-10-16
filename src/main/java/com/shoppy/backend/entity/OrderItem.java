package com.shoppy.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Chi tiết sản phẩm trong đơn hàng.
 * Sử dụng cơ chế SNAPSHOT: lưu lại tên, giá tại thời điểm đặt hàng
 * để lịch sử mua hàng không bị ảnh hưởng khi sản phẩm thay đổi sau này.
 *
 * Lưu ý column mapping:
 *  - product_name_snapshot → Java field (Hibernate ADD mới)
 *  - product_name          → cột CŨ trong DB (NOT NULL, cần fill)
 *  Tương tự cho variant_name và unit_price.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Liên kết đến đơn hàng ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ── Tham chiếu gốc ────────────────────────────────────────────────────────
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_variant_id", nullable = false)
    private Long productVariantId;

    // Cột legacy (schema cũ, nullable)
    @Column(name = "variant_id")
    private Long variantId;

    // Ảnh sản phẩm tại thời điểm đặt hàng (snapshot, nullable)
    @Column(name = "product_image")
    private String productImage;

    // ── SNAPSHOT: Lưu tên & giá tại thời điểm đặt hàng ──────────────────────
    // Cột mới (Hibernate ADD thêm)
    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    // Cột CŨ trong DB schema gốc - giữ cùng giá trị với productNameSnapshot
    @Column(name = "product_name", nullable = false)
    private String productName;

    // Cột mới (Hibernate ADD thêm)
    @Column(name = "variant_name_snapshot", nullable = false)
    private String variantNameSnapshot;

    // Cột CŨ trong DB schema gốc - giữ cùng giá trị với variantNameSnapshot
    @Column(name = "variant_name")
    private String variantName;

    // Cột mới (Hibernate ADD thêm)
    @Column(name = "price_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal priceSnapshot;

    // Cột CŨ trong DB schema gốc - giữ cùng giá trị với priceSnapshot
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    // ── Số lượng ─────────────────────────────────────────────────────────────
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // ── Tổng tiền dòng (priceSnapshot * quantity) ────────────────────────────
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
}
