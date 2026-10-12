package com.shoppy.backend.service;

import com.shoppy.backend.domain.request.shop.RegisterShopRequest;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${app.default-avatar-url:https://res.cloudinary.com/dii10rze6/image/upload/v1790846872/shoppy_avatars/vzsefqbb6qlp0zqtx3yy.png}")
    private String defaultLOGOUrl;

    @Transactional
    public ShopResponse registerShop(String email, RegisterShopRequest request, MultipartFile logo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // 1. Kiểm tra 1-to-1: User đã đăng ký Shop chưa
        if (shopRepository.existsByUserIdAndIsDeletedFalse(user.getId())) {
            throw new RuntimeException("Tài khoản của bạn đã sở hữu một Cửa hàng!");
        }

        // 2. Kiểm tra trùng tên Shop
        if (shopRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new RuntimeException("Tên Shop đã được sử dụng, vui lòng chọn tên khác!");
        }

        // XỬ LÝ ẢNH LOGO SHOP MẶC ĐỊNH:
        String logoUrl = defaultLOGOUrl; // Hoặc gán bằng null / một link ảnh mặc định hệ thống nếu có
        if (logo != null && !logo.isEmpty()) {
            // Nếu cửa hàng có gửi file ảnh hợp lệ -> Upload lên Cloudinary vào thư mục shop logos
            logoUrl = cloudinaryService.uploadImage(logo, "shoppy_shop_logos");
        }

        // 4. Khởi tạo thông tin Shop
        Shop shop = Shop.builder()
                .userId(user.getId())
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(logoUrl)
                .googleMapUrl(request.getGoogleMapUrl())
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        Shop savedShop = shopRepository.save(shop);

        // 5. Cập nhật Role của User thành SELLER (nếu đang là USER)
        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.SELLER);
            userRepository.save(user);
        }

        return ShopResponse.fromEntity(savedShop);
    }

    public ShopResponse getMyShop(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký Shop nào!"));

        return ShopResponse.fromEntity(shop);
    }
}