package com.shoppy.backend.repository;

import com.shoppy.backend.entity.WalletDeposit;
import com.shoppy.backend.common.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletDepositRepository extends JpaRepository<WalletDeposit, Long> {
    List<WalletDeposit> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WalletDeposit> findByStatus(TransactionStatus status);
}