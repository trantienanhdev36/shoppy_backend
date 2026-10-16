package com.shoppy.backend.repository;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm đơn hàng kèm PESSIMISTIC_WRITE lock để chống race condition
     * khi nhiều worker cùng xử lý một đơn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

    /** Lấy toàn bộ đơn hàng của một khách hàng */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Lấy danh sách đơn theo trạng thái (dùng cho Seller) */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /** Lấy đơn của shipper đang giao */
    List<Order> findByShipperIdAndStatusOrderByCreatedAtDesc(Long shipperId, OrderStatus status);
}
