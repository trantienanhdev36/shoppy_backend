package com.shoppy.backend.repository;

import com.shoppy.backend.common.enums.ShipperTransactionType;
import com.shoppy.backend.entity.ShipperCodTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ShipperCodTransactionRepository extends JpaRepository<ShipperCodTransaction, Long> {
    List<ShipperCodTransaction> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
    List<ShipperCodTransaction> findByShipperIdAndTypeOrderByCreatedAtDesc(Long shipperId, ShipperTransactionType type);
    List<ShipperCodTransaction> findByOrderId(Long orderId);

    /** Tổng tiền COD theo loại giao dịch (COLLECTION hoặc REMITTANCE) */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM ShipperCodTransaction t WHERE t.shipperId = :shipperId AND t.type = :type AND t.isDeleted = false")
    BigDecimal sumAmountByShipperIdAndType(@Param("shipperId") Long shipperId, @Param("type") ShipperTransactionType type);
}
