package com.shoppy.backend.repository;

import com.shoppy.backend.entity.ShopBalanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopBalanceTransactionRepository extends JpaRepository<ShopBalanceTransaction, Long> {
    boolean existsByOrderId(Long orderId);
}
