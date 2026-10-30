package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.domain.request.user.ChangePasswordRequest;
import com.shoppy.backend.domain.request.user.RegisterRequest;
import com.shoppy.backend.domain.request.user.UpdateProfileRequest;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.entity.VerificationToken;
import com.shoppy.backend.repository.UserRepository;
import com.shoppy.backend.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    @Value("${app.default-avatar-url:https://res.cloudinary.com/dii10rze6/image/upload/v1790846872/shoppy_avatars/vzsefqbb6qlp0zqtx3yy.png}")
    private String defaultAvatarUrl;

    @Transactional
    public User register(RegisterRequest request, MultipartFile avatar) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTED);
        }

        //  XỬ LÝ ẢNH MẶC ĐỊNH:
        String avatarUrl = defaultAvatarUrl; // Mặc định gán ảnh hệ thống trước
        if (avatar != null && !avatar.isEmpty()) {
            // Nếu người dùng có gửi file ảnh hợp lệ -> Upload lên Cloudinary và lấy URL mới
            avatarUrl = cloudinaryService.uploadImage(avatar, "shoppy_avatars");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER) // Mọi user đăng ký mặc định là CUSTOMER
                .status(UserStatus.INACTIVE)
                .balance(BigDecimal.ZERO)
                .shippingCompany(null)
                .avatarUrl(avatarUrl) //  Đảm bảo luôn có URL (ảnh upload hoặc ảnh mặc định)
                .build();

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Tạo Verification Token & Gửi Mail kích hoạt
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);

// CHỈ TRUYỀN TOKEN THÔ, KHÔNG NỐI URL Ở ĐÂY
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFullName(), token);
        return savedUser;
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân của người dùng (họ tên, số điện thoại, avatar).
     */
    @Transactional
    public User updateProfile(String email, UpdateProfileRequest request, MultipartFile avatar) {
        User user = getUserByEmail(email);

        // 1. Kiểm tra số điện thoại (nếu đổi số điện thoại mới thì không được trùng với tài khoản khác)
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String newPhone = request.getPhone().trim();
            if (!newPhone.equals(user.getPhone())) {
                if (userRepository.existsByPhoneAndIdNot(newPhone, user.getId())) {
                    throw new AppException(ErrorCode.PHONE_EXISTED);
                }
                user.setPhone(newPhone);
            }
        } else if (request.getPhone() != null && request.getPhone().isBlank()) {
            user.setPhone(null);
        }

        // 2. Cập nhật họ tên
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        // 3. Cập nhật ảnh đại diện mới nếu có upload
        if (avatar != null && !avatar.isEmpty()) {
            String newAvatarUrl = cloudinaryService.uploadImage(avatar, "shoppy_avatars");
            user.setAvatarUrl(newAvatarUrl);
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Đổi mật khẩu tài khoản người dùng.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}