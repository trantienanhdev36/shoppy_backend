package com.shoppy.backend.service;

import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.WalletDeposit;
import com.shoppy.backend.common.enums.TransactionStatus;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.WalletDepositRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final StringRedisTemplate redisTemplate;

    public static final String QUEUE_KEY = "queue:deposits";

    @Transactional
    public UserResponse.WalletDepositResponse createDepositRequest(String email, BigDecimal amount) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 1. Tạo bản ghi giao dịch ở trạng thái PENDING
        WalletDeposit deposit = WalletDeposit.builder()
                .userId(user.getId())
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .build();

        WalletDeposit savedDeposit = walletDepositRepository.save(deposit);

        // 2. Đẩy ID giao dịch vào Redis Queue (RPUSH)
        redisTemplate.opsForList().rightPush(QUEUE_KEY, savedDeposit.getId().toString());
        log.info("Đã đẩy depositId [{}] vào Redis Queue", savedDeposit.getId());

        // 3. Trả về DTO trạng thái PENDING ngay cho Client
        return UserResponse.WalletDepositResponse.fromEntity(savedDeposit);
    }

}