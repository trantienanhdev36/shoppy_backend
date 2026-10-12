package com.shoppy.backend.common.util;

import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.WalletDeposit;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.WalletDepositRepository;
import com.shoppy.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositQueueWorker {

    private final StringRedisTemplate redisTemplate;
    private final WalletDepositRepository walletDepositRepository;
    private final UserRepository userRepository;

    // Quét Redis Queue mỗi 500ms (0.5 giây)
    @Scheduled(fixedDelay = 500)
    public void consumeQueue() {
        // Lấy phần tử đầu tiên ra khỏi Redis List (LPOP)
        String depositIdStr = redisTemplate.opsForList().leftPop(WalletService.QUEUE_KEY);

        if (depositIdStr != null) {
            Long depositId = Long.parseLong(depositIdStr);
            log.info("Worker lấy thành công depositId [{}] từ Redis Queue để xử lý", depositId);
            processDepositTransaction(depositId);
        }
    }

    @Transactional
    public void processDepositTransaction(Long depositId) {
        try {
            WalletDeposit deposit = walletDepositRepository.findById(depositId).orElse(null);
            if (deposit == null || deposit.getStatus() != TransactionStatus.PENDING) {
                return;
            }

            User user = userRepository.findById(deposit.getUserId()).orElse(null);
            if (user == null) {
                deposit.setStatus(TransactionStatus.FAILED);
                walletDepositRepository.save(deposit);
                return;
            }

            // Cộng tiền tài khoản
            BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
            user.setBalance(currentBalance.add(deposit.getAmount()));

            // Cập nhật trạng thái giao dịch
            deposit.setStatus(TransactionStatus.COMPLETED);

            userRepository.save(user);
            walletDepositRepository.save(deposit);

            log.info("Xử lý nạp tiền thành công từ Queue cho depositId [{}]. Số dư mới: {}", depositId, user.getBalance());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý nạp tiền cho depositId [{}]: {}", depositId, e.getMessage());
        }
    }
}