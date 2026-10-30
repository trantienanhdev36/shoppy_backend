package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.NotificationType;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.domain.response.wallet.WalletTransactionResponse;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.WalletDeposit;
import com.shoppy.backend.entity.WalletTransaction;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.common.enums.WalletTransactionType;
import com.shoppy.backend.repository.WalletDepositRepository;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    public static final String WALLET_QUEUE_KEY = "queue:wallet";
    public static final String ORDER_QUEUE_KEY = "queue:orders";

    @Transactional
    public UserResponse.WalletDepositResponse createDepositRequest(String email, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        WalletDeposit deposit = WalletDeposit.builder()
                .userId(user.getId())
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .build();
        WalletDeposit savedDeposit = walletDepositRepository.save(deposit);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(user.getId());
        transaction.setAmount(amount);
        transaction.setLegacyDepositId(savedDeposit.getId());
        transaction.setType(WalletTransactionType.CREDIT);
        transaction.setStatus(TransactionStatus.PENDING);

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        enqueueAfterCommit(WALLET_QUEUE_KEY, savedTransaction.getId());

        return UserResponse.WalletDepositResponse.fromEntity(savedTransaction);
    }

    /**
     * Lấy danh sách lịch sử giao dịch ví của user hiện tại (có phân trang và lọc theo type).
     */
    @Transactional(readOnly = true)
    public PagedResponse<WalletTransactionResponse> getMyWalletTransactions(
            String email, WalletTransactionType type, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> type != null
                        ? walletTransactionRepository.findByUserIdAndTypeAndIsDeletedFalse(user.getId(), type, pageable)
                        : walletTransactionRepository.findByUserIdAndIsDeletedFalse(user.getId(), pageable),
                WalletTransactionResponse::fromEntity
        );
    }

    @Transactional
    public void createDebitRequest(Long userId, Long orderId, BigDecimal amount) {
        WalletTransaction transaction = walletTransactionRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    WalletTransaction newTransaction = new WalletTransaction();
                    newTransaction.setUserId(userId);
                    newTransaction.setOrderId(orderId);
                    newTransaction.setAmount(amount);
                    newTransaction.setType(WalletTransactionType.DEBIT);
                    newTransaction.setStatus(TransactionStatus.PENDING);
                    return walletTransactionRepository.save(newTransaction);
                });

        if (!transaction.getUserId().equals(userId)
                || transaction.getType() != WalletTransactionType.DEBIT
                || transaction.getAmount().compareTo(amount) != 0) {
            throw new IllegalStateException("Giao dịch ví của đơn hàng không khớp orderId " + orderId);
        }

        if (transaction.getStatus() == TransactionStatus.PENDING) {
            enqueueAfterCommit(WALLET_QUEUE_KEY, transaction.getId());
        } else {
            enqueueAfterCommit(ORDER_QUEUE_KEY, orderId);
        }
    }

    @Transactional
    public void processTransaction(Long transactionId) {
        WalletTransaction transaction = walletTransactionRepository.findByIdWithLock(transactionId)
                .orElse(null);
        if (transaction == null || transaction.getStatus() != TransactionStatus.PENDING) {
            return;
        }

        User user = userRepository.findByIdWithLock(transaction.getUserId()).orElse(null);
        if (user == null) {
            transaction.setStatus(TransactionStatus.FAILED);
            updateLegacyDepositStatus(transaction, TransactionStatus.FAILED);
            walletTransactionRepository.save(transaction);
            enqueueOrderAfterCommit(transaction);
            return;
        }

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        if (transaction.getType() == WalletTransactionType.DEBIT
                && currentBalance.compareTo(transaction.getAmount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            updateLegacyDepositStatus(transaction, TransactionStatus.FAILED);
            walletTransactionRepository.save(transaction);
            enqueueOrderAfterCommit(transaction);
            log.info("[WalletQueue] Không đủ số dư cho orderId [{}]", transaction.getOrderId());
            return;
        }

        BigDecimal newBalance = transaction.getType() == WalletTransactionType.CREDIT
                ? currentBalance.add(transaction.getAmount())
                : currentBalance.subtract(transaction.getAmount());
        user.setBalance(newBalance);
        transaction.setStatus(TransactionStatus.COMPLETED);
        updateLegacyDepositStatus(transaction, TransactionStatus.COMPLETED);
        userRepository.save(user);
        walletTransactionRepository.save(transaction);
        enqueueOrderAfterCommit(transaction);

        if (transaction.getType() == WalletTransactionType.CREDIT) {
            notificationService.sendNotification(
                    user.getId(),
                    NotificationType.WALLET_DEPOSIT,
                    "Nạp tiền ví thành công",
                    String.format("Bạn đã nạp thành công %,.0f VND vào ví Shoppy. Số dư hiện tại: %,.0f VND.",
                            transaction.getAmount().doubleValue(), newBalance.doubleValue()),
                    transaction.getId()
            );
        }

        log.info("[WalletQueue] Giao dịch [{}] {} hoàn tất. Số dư mới: {}",
                transactionId, transaction.getType(), newBalance);
    }

    /**
     * Hoàn tiền cho đơn hàng bị hủy vào ví của khách hàng.
     * Cập nhật số dư, lưu bản ghi giao dịch CREDIT và gửi thông báo cho khách.
     *
     * @param customerId ID khách hàng nhận hoàn tiền
     * @param orderId    ID đơn hàng bị hủy
     * @param amount     Tổng số tiền hoàn (bao gồm tiền hàng + phí ship nếu có)
     * @param reason     Lý do hủy đơn
     */
    @Transactional
    public void refundOrder(Long customerId, Long orderId, BigDecimal amount, String reason) {
        User user = userRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(amount);
        user.setBalance(newBalance);
        userRepository.save(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(customerId);
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.CREDIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        walletTransactionRepository.save(transaction);

        // Bắn thông báo hoàn tiền vào ví
        notificationService.sendNotification(
                user.getId(),
                NotificationType.WALLET_DEPOSIT,
                "Hoàn tiền đơn hàng #" + orderId,
                String.format("Bạn đã được hoàn %,.0f VND vào ví Shoppy cho đơn hàng #%d. Lý do: %s. Số dư hiện tại: %,.0f VND.",
                        amount.doubleValue(), orderId, reason, newBalance.doubleValue()),
                orderId
        );

        log.info("[WalletService] Đã hoàn tiền cho đơn [{}] vào ví user [{}]: {} VND. Số dư mới: {}",
                orderId, customerId, amount, newBalance);
    }

    @Transactional
    public void requeuePendingTransactions() {
        for (WalletDeposit deposit : walletDepositRepository.findByStatus(TransactionStatus.PENDING)) {
            if (walletTransactionRepository.findByLegacyDepositId(deposit.getId()).isEmpty()) {
                WalletTransaction transaction = new WalletTransaction();
                transaction.setUserId(deposit.getUserId());
                transaction.setLegacyDepositId(deposit.getId());
                transaction.setAmount(deposit.getAmount());
                transaction.setType(WalletTransactionType.CREDIT);
                transaction.setStatus(TransactionStatus.PENDING);
                walletTransactionRepository.save(transaction);
            }
        }

        walletTransactionRepository.findByStatus(TransactionStatus.PENDING)
                .forEach(transaction -> enqueueAfterCommit(WALLET_QUEUE_KEY, transaction.getId()));
    }

    private void updateLegacyDepositStatus(WalletTransaction transaction, TransactionStatus status) {
        if (transaction.getLegacyDepositId() == null) {
            return;
        }
        walletDepositRepository.findById(transaction.getLegacyDepositId()).ifPresent(deposit -> {
            deposit.setStatus(status);
            walletDepositRepository.save(deposit);
        });
    }

    private void enqueueOrderAfterCommit(WalletTransaction transaction) {
        if (transaction.getType() == WalletTransactionType.DEBIT && transaction.getOrderId() != null) {
            enqueueAfterCommit(ORDER_QUEUE_KEY, transaction.getOrderId());
        }
    }

    private void enqueueAfterCommit(String queueKey, Long id) {
        Runnable enqueue = () -> {
            String queuedKey = queueKey + ":queued:" + id;
            try {
                Boolean claimed = redisTemplate.opsForValue()
                        .setIfAbsent(queuedKey, "1", Duration.ofMinutes(1));
                if (!Boolean.TRUE.equals(claimed)) {
                    return;
                }
                redisTemplate.opsForList().rightPush(queueKey, id.toString());
            } catch (RuntimeException e) {
                redisTemplate.delete(queuedKey);
                log.error("[WalletQueue] Không thể đưa id [{}] vào queue [{}]; worker recovery sẽ thử lại",
                        id, queueKey, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.run();
                }
            });
        } else {
            enqueue.run();
        }
    }
}