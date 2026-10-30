package com.shoppy.backend.repository;

import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.WalletTransactionType;
import com.shoppy.backend.entity.WalletTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tx FROM WalletTransaction tx WHERE tx.id = :id")
    Optional<WalletTransaction> findByIdWithLock(@Param("id") Long id);

    Optional<WalletTransaction> findByOrderId(Long orderId);

    Optional<WalletTransaction> findByLegacyDepositId(Long legacyDepositId);

    List<WalletTransaction> findByStatus(TransactionStatus status);

    Page<WalletTransaction> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<WalletTransaction> findByUserIdAndTypeAndIsDeletedFalse(Long userId, WalletTransactionType type, Pageable pageable);
}
