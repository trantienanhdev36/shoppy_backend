package com.shoppy.backend.repository;

import com.shoppy.backend.common.enums.OrderStatus;
import com.shoppy.backend.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Lấy toàn bộ đơn hàng của một khách hàng (không phân trang — giữ lại để tương thích) */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Lấy đơn hàng của một khách hàng — có phân trang.
     * Sort được truyền qua Pageable (thường dùng Sort.by(DESC, "createdAt")).
     */
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /** Lấy danh sách đơn theo trạng thái (dùng cho Seller) */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    /** Lấy đơn hàng theo trạng thái (toàn sàn cho Admin) — có phân trang */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Lấy đơn hàng của Shop theo trạng thái — có phân trang */
    Page<Order> findByShopIdAndStatus(Long shopId, OrderStatus status, Pageable pageable);

    /** Lấy toàn bộ đơn hàng của Shop — có phân trang */
    Page<Order> findByShopId(Long shopId, Pageable pageable);

    /** Lấy danh sách đơn hàng của Shop theo trạng thái (không phân trang) */
    List<Order> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, OrderStatus status);

    /** Lấy danh sách đơn có sẵn để nhận (READY_TO_SHIP) theo hãng vận chuyển */
    List<Order> findByShippingCompanyIdAndStatusOrderByCreatedAtAsc(Long shippingCompanyId, OrderStatus status);

    /** Lấy chi tiết 1 đơn hàng theo id + shippingCompanyId (shipper chỉ xem đơn thuộc hãng mình) */
    Optional<Order> findByIdAndShippingCompanyId(Long id, Long shippingCompanyId);

    /** Lấy đơn của shipper đang giao hoặc đã giao */
    List<Order> findByShipperIdAndStatusOrderByCreatedAtDesc(Long shipperId, OrderStatus status);

    /** Lấy tất cả đơn của shipper (mọi trạng thái) */
    List<Order> findByShipperIdOrderByCreatedAtDesc(Long shipperId);

    /** Đếm tổng số đơn hàng của khách hàng */
    long countByCustomerId(Long customerId);

    /** Đếm tổng số đơn hàng của Cửa hàng */
    long countByShopId(Long shopId);
}
