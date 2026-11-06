package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.domain.request.shop.RegisterShopRequest;
import com.shoppy.backend.domain.request.shop.UpdateShopRequest;
import com.shoppy.backend.domain.response.product.ProductResponseDTO;
import com.shoppy.backend.domain.response.shop.ShopPublicProfileResponse;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.User;
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
    private final ProductService productService;

    @Value("${app.default-avatar-url:https://res.cloudinary.com/dii10rze6/image/upload/v1790846872/shoppy_avatars/vzsefqbb6qlp0zqtx3yy.png}")
    private String defaultLOGOUrl;

    @Transactional
    public ShopResponse registerShop(String email, RegisterShopRequest request, MultipartFile logo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Kiểm tra 1-to-1: User đã đăng ký Shop chưa
        if (shopRepository.existsByUserIdAndIsDeletedFalse(user.getId())) {
            throw new AppException(ErrorCode.SHOP_NAME_EXISTED);
        }

        // 2. Kiểm tra trùng tên Shop
        if (shopRepository.existsByNameAndIsDeletedFalse(request.getName())) {
            throw new AppException(ErrorCode.SHOP_NAME_EXISTED);
        }

        // 3. Xử lý ảnh logo shop
        String logoUrl = defaultLOGOUrl;
        if (logo != null && !logo.isEmpty()) {
            logoUrl = cloudinaryService.uploadImage(logo, "shoppy_shop_logos");
        }

        // 4. Khởi tạo thông tin Shop
        Shop shop = Shop.builder()
                .userId(user.getId())
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(logoUrl)
                .address(request.getAddress())
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .build();

        Shop savedShop = shopRepository.save(shop);

        // 5. Cập nhật Role của User thành SELLER (nếu đang là CUSTOMER)
        if (user.getRole() == UserRole.CUSTOMER) {
            user.setRole(UserRole.SELLER);
            userRepository.save(user);
        }

        return ShopResponse.fromEntity(savedShop);
    }

    @Transactional(readOnly = true)
    public ShopResponse getMyShop(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        return ShopResponse.fromEntity(shop);
    }

    /**
     * Cập nhật thông tin Cửa hàng của Seller (tên, mô tả, địa chỉ, logo mới).
     */
    @Transactional
    public ShopResponse updateMyShop(String email, UpdateShopRequest request, MultipartFile logo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // 1. Kiểm tra nếu đổi tên shop thì tên mới không được trùng với shop khác
        if (!shop.getName().equals(request.getName())) {
            if (shopRepository.existsByNameAndIsDeletedFalseAndIdNot(request.getName(), shop.getId())) {
                throw new AppException(ErrorCode.SHOP_NAME_EXISTED);
            }
            shop.setName(request.getName());
        }

        // 2. Cập nhật mô tả và địa chỉ
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress());

        // 3. Cập nhật logo mới nếu có upload
        if (logo != null && !logo.isEmpty()) {
            String newLogoUrl = cloudinaryService.uploadImage(logo, "shoppy_shop_logos");
            shop.setLogoUrl(newLogoUrl);
        }

        Shop savedShop = shopRepository.save(shop);
        return ShopResponse.fromEntity(savedShop);
    }

    /**
     * Lấy thông tin trang profile Public của Cửa hàng dành cho khách mua hàng xem,
     * bao gồm thông tin Shop và danh sách sản phẩm Shop đang bán (phân trang).
     */
    @Transactional(readOnly = true)
    public ShopPublicProfileResponse getPublicShopProfile(Long shopId, int page, int size) {
        Shop shop = shopRepository.findByIdAndIsDeletedFalse(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if ("INACTIVE".equalsIgnoreCase(shop.getStatus()) || "BANNED".equalsIgnoreCase(shop.getStatus())) {
            throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        PagedResponse<ProductResponseDTO> products = productService.getPublicProductsByShop(shopId, page, size);

        return ShopPublicProfileResponse.fromEntity(shop, products);
    }
}