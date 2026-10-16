package com.shoppy.backend.repository;

import com.shoppy.backend.entity.ShipperCodTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipperCodTransactionRepository extends JpaRepository<ShipperCodTransaction, Long> {
    List<ShipperCodTransaction> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
    List<ShipperCodTransaction> findByOrderId(Long orderId);
}
