package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.NotificationType;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.admin.UpdateShopStatusRequest;
import com.shoppy.backend.domain.response.admin.AdminShopDetailResponse;
import com.shoppy.backend.domain.response.shop.ShopResponse;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.repository.OrderRepository;
import com.shoppy.backend.repository.ProductRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    private static final Set<String> VALID_SHOP_STATUSES = Set.of("ACTIVE", "INACTIVE", "BLOCKED");

    /**
     * Lấy danh sách tất cả Cửa hàng trên sàn (có phân trang, tìm kiếm & lọc).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ShopResponse> getAllShops(String search, String status, int page, int size) {
        String cleanSearch = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> shopRepository.findShopsByAdmin(cleanSearch, cleanStatus, pageable),
                ShopResponse::fromEntity
        );
    }

    /**
     * Xem thông tin chi tiết một Cửa hàng (kèm thông tin Chủ shop, số sản phẩm, số đơn hàng).
     */
    @Transactional(readOnly = true)
    public AdminShopDetailResponse getShopDetail(Long id) {
        Shop shop = shopRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        User owner = userRepository.findById(shop.getUserId()).orElse(null);
        long totalProducts = productRepository.countByShopIdAndIsDeletedFalse(id);
        long totalOrders = orderRepository.countByShopId(id);

        return AdminShopDetailResponse.fromEntity(shop, owner, totalProducts, totalOrders);
    }

    /**
     * Cập nhật trạng thái Cửa hàng (ACTIVE, INACTIVE, BLOCKED).
     */
    @Transactional
    public ShopResponse updateShopStatus(Long id, UpdateShopStatusRequest request, String adminEmail) {
        String newStatus = request.getStatus().trim().toUpperCase();
        if (!VALID_SHOP_STATUSES.contains(newStatus)) {
            throw new AppException(ErrorCode.INVALID_SHOP_STATUS);
        }

        Shop shop = shopRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        String oldStatus = shop.getStatus();
        shop.setStatus(newStatus);
        Shop savedShop = shopRepository.save(shop);

        // Gửi thông báo in-app cho chủ shop
        String statusLabel = switch (newStatus) {
            case "ACTIVE" -> "được kích hoạt hoạt động trở lại";
            case "BLOCKED" -> "bị khóa do vi phạm chính sách sàn";
            case "INACTIVE" -> "tạm ngưng hoạt động";
            default -> newStatus;
        };

        String reasonMsg = (request.getReason() != null && !request.getReason().trim().isEmpty())
                ? " Lý do: " + request.getReason().trim()
                : "";

        notificationService.sendNotification(
                shop.getUserId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Cập nhật trạng thái Cửa hàng",
                "Cửa hàng [" + shop.getName() + "] của bạn đã " + statusLabel + " bởi Quản trị viên." + reasonMsg,
                shop.getId()
        );

        log.info("[AdminShopService] Admin [{}] đã cập nhật trạng thái Shop [{}] từ [{}] sang [{}]. Lý do: {}",
                adminEmail, shop.getName(), oldStatus, newStatus, request.getReason());

        return ShopResponse.fromEntity(savedShop);
    }
}
