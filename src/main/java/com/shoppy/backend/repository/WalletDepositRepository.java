package com.shoppy.backend.repository;

import com.shoppy.backend.entity.WalletDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletDepositRepository extends JpaRepository<WalletDeposit, Long> {
    List<WalletDeposit> findByUserIdOrderByCreatedAtDesc(Long userId);
}