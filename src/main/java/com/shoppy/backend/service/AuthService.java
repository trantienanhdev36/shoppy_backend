package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.JwtUtils;
import com.shoppy.backend.dto.auth.LoginRequest;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest request) {
        // 1. Kiểm tra Email có tồn tại không
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra Mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        // 3. Kiểm tra Trạng thái Kích hoạt của Tài khoản
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVATED);
        }

        // 4. Sinh mã JWT Token
        String accessToken = jwtUtils.generateToken(user);

        // 5. Đóng gói Token + Entity User vào Map để trả về Controller
        Map<String, Object> authResult = new HashMap<>();
        authResult.put("accessToken", accessToken);
        authResult.put("tokenType", "Bearer");
        authResult.put("user", user); // Entity User (không chứa password nhờ @JsonProperty)

        return authResult;
    }
}