package com.shoppy.backend.common.util;

import com.shoppy.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletQueueWorker {

    private final StringRedisTemplate redisTemplate;
    private final WalletService walletService;

    @Scheduled(fixedDelay = 500)
    public void consumeQueue() {
        String transactionId = redisTemplate.opsForList().leftPop(WalletService.WALLET_QUEUE_KEY);
        if (transactionId == null) {
            return;
        }

        redisTemplate.delete(WalletService.WALLET_QUEUE_KEY + ":queued:" + transactionId);
        try {
            walletService.processTransaction(Long.parseLong(transactionId));
        } catch (RuntimeException e) {
            log.error("[WalletQueueWorker] Lỗi xử lý giao dịch [{}]; giao dịch sẽ được recovery",
                    transactionId, e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void recoverPendingTransactions() {
        walletService.requeuePendingTransactions();
    }
}
