package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.JwtUtils;
import com.shoppy.backend.dto.auth.AuthResponse;
import com.shoppy.backend.dto.auth.LoginRequest;
import com.shoppy.backend.dto.user.UserResponse;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.VerificationToken;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Kiểm tra Email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra Mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        // 3. Kiểm tra Trạng thái kích hoạt
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }

        // 4. Sinh mã JWT Access Token
        String accessToken = jwtUtils.generateToken(user);

        // 5. Chuyển đổi User Entity sang UserResponse DTO
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .status(user.getStatus())
                .build();

        // 6. Trả về AuthResponse Model chuẩn RESTful
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    private final VerificationTokenRepository tokenRepository; //  Inject Repository lưu UUID Token

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

            // 4. Xóa Token sau khi đã dùng xong
            tokenRepository.delete(verificationToken);

            log.info("Tài khoản {} đã kích hoạt thành công", user.getEmail());
            return true;

        } catch (Exception e) {
            log.error("Lỗi khi xác thực tài khoản: {}", e.getMessage());
            return false;
        }
    }
}