package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.JwtUtils;
import com.shoppy.backend.domain.request.auth.ForgotPasswordRequest;
import com.shoppy.backend.domain.request.auth.LoginRequest;
import com.shoppy.backend.domain.request.auth.ResetPasswordRequest;
import com.shoppy.backend.domain.response.auth.AuthResponse;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.VerificationToken;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoppy.backend.common.enums.NotificationType;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final VerificationTokenRepository tokenRepository;

    private static final String RESET_PWD_OTP_PREFIX = "otp:reset_pwd:";
    private static final String RESET_PWD_COOLDOWN_PREFIX = "cooldown:reset_pwd:";

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Kiểm tra Email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra Mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        // 3. Kiểm tra Trạng thái tài khoản
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 4. Sinh mã JWT Access Token
        String accessToken = jwtUtils.generateToken(user);

        // 5. Chuyển đổi User Entity sang UserResponse DTO
        UserResponse userResponse = UserResponse.fromEntity(user);

        // 6. Trả về AuthResponse Model chuẩn RESTful
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Transactional
    public boolean verifyAccount(String tokenStr) {
        try {
            // 1. Tìm Token trong Database theo chuỗi UUID
            VerificationToken verificationToken = tokenRepository.findByToken(tokenStr)
                    .orElse(null);

            if (verificationToken == null) {
                log.error("Xác thực thất bại: Token không tồn tại trong hệ thống");
                return false;
            }

            // 2. Kiểm tra Token đã hết hạn chưa (24h)
            if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                log.error("Xác thực thất bại: Token đã hết hạn");
                tokenRepository.delete(verificationToken); // Dọn dẹp token hết hạn
                return false;
            }

            // 3. Kích hoạt tài khoản User
            User user = verificationToken.getUser();
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            // 4. Gửi thông báo in-app
            notificationService.sendNotification(
                    user.getId(),
                    NotificationType.EMAIL_VERIFIED,
                    "Kích hoạt tài khoản thành công",
                    "Chúc mừng bạn đã xác thực email thành công! Bây giờ bạn có thể trải nghiệm mua sắm trên Shoppy.",
                    user.getId()
            );

            // 5. Xóa Token sau khi đã dùng xong
            tokenRepository.delete(verificationToken);

            log.info("Tài khoản {} đã kích hoạt thành công", user.getEmail());
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi xác thực tài khoản: {}", e.getMessage());
            return false;
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        // 1. Kiểm tra User có tồn tại không
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_BLOCKED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }

        // 2. Kiểm tra Cooldown chống spam gửi mã liên tục (60 giây)
        String cooldownKey = RESET_PWD_COOLDOWN_PREFIX + request.getEmail();
        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            throw new AppException(ErrorCode.OTP_COOLDOWN);
        }

        // 3. Sinh mã OTP ngẫu nhiên 6 chữ số
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));

        // 4. Lưu OTP và Cooldown vào Redis
        String otpKey = RESET_PWD_OTP_PREFIX + request.getEmail();
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(60));

        // 5. Gửi email chứa mã OTP bất đồng bộ
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Đã tạo và gửi OTP đặt lại mật khẩu cho email: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Kiểm tra User có tồn tại không
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Lấy OTP từ Redis và kiểm tra
        String otpKey = RESET_PWD_OTP_PREFIX + request.getEmail();
        String cachedOtp = redisTemplate.opsForValue().get(otpKey);

        if (cachedOtp == null) {
            throw new AppException(ErrorCode.EXPIRED_OTP);
        }

        if (!cachedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // 3. Cập nhật mật khẩu mới đã mã hóa BCrypt
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 4. Xóa OTP và Cooldown khỏi Redis
        redisTemplate.delete(otpKey);
        String cooldownKey = RESET_PWD_COOLDOWN_PREFIX + request.getEmail();
        redisTemplate.delete(cooldownKey);

        log.info("Đặt lại mật khẩu thành công cho email: {}", user.getEmail());
    }
}