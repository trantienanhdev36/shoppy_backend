package com.shoppy.backend.entity;

import com.shoppy.backend.common.enums.TrackingStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Lịch sử theo dõi trạng thái đơn hàng.
 * Mỗi lần trạng thái thay đổi → tạo một bản ghi mới.
 */
@Entity
@Table(name = "order_trackings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TrackingStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_by")
    private Long updatedBy; // userId của người thực hiện thay đổi (worker, seller, shipper)
}
