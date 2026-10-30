package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.NotificationType;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.admin.UpdateUserRoleRequest;
import com.shoppy.backend.domain.request.admin.UpdateUserStatusRequest;
import com.shoppy.backend.domain.response.admin.AdminUserDetailResponse;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.entity.Shop;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.repository.OrderRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Lấy danh sách người dùng toàn sàn có phân trang, tìm kiếm & lọc.
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(
            String search,
            UserRole role,
            UserStatus status,
            int page,
            int size
    ) {
        String cleanSearch = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> userRepository.findUsersByAdmin(cleanSearch, role, status, pageable),
                UserResponse::fromEntity
        );
    }

    /**
     * Xem thông tin chi tiết một người dùng bất kỳ (kèm Shop nếu là Seller, ShippingCompany nếu là Shipper).
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId()).orElse(null);
        long totalOrders = orderRepository.countByCustomerId(user.getId());

        return AdminUserDetailResponse.fromEntity(user, shop, totalOrders);
    }

    /**
     * Cập nhật trạng thái người dùng (Khóa / Mở khóa / Kích hoạt).
     */
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Không cho phép Admin tự vô hiệu hóa / khóa chính mình (chỉ cho phép nếu giữ ACTIVE)
        if (targetUser.getId().equals(admin.getId()) && request.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        // 2. Không cho phép vô hiệu hóa / khóa bất kỳ tài khoản nào mang vai trò ADMIN
        if (targetUser.getRole() == UserRole.ADMIN && request.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_ADMIN);
        }

        // 3. Cập nhật trạng thái
        targetUser.setStatus(request.getStatus());
        userRepository.save(targetUser);

        // 4. Nếu khóa User (BLOCKED) và user là SELLER -> Tạm dừng Shop liên kết
        if (request.getStatus() == UserStatus.BLOCKED && targetUser.getRole() == UserRole.SELLER) {
            shopRepository.findByUserIdAndIsDeletedFalse(targetUser.getId()).ifPresent(shop -> {
                shop.setStatus("INACTIVE");
                shopRepository.save(shop);
                log.info("[AdminUserService] Tạm ngừng hoạt động Shop [{}] do chủ shop bị khóa", shop.getName());
            });
        }

        // 5. Gửi thông báo in-app cho người dùng
        String statusLabel = switch (request.getStatus()) {
            case ACTIVE -> "được kích hoạt";
            case BLOCKED -> "bị tạm khóa";
            case INACTIVE -> "chuyển sang tạm ngưng";
            case PENDING -> "chuyển sang chờ xác thực";
        };
        String reasonDetail = (request.getReason() != null && !request.getReason().trim().isEmpty())
                ? " Lý do: " + request.getReason().trim()
                : "";

        notificationService.sendNotification(
                targetUser.getId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Cập nhật trạng thái tài khoản",
                "Tài khoản của bạn đã " + statusLabel + " bởi Quản trị viên hệ thống." + reasonDetail,
                targetUser.getId()
        );

        log.info("[AdminUserService] Admin [{}] đã cập nhật trạng thái User [{}] thành [{}]. Lý do: {}",
                adminEmail, targetUser.getEmail(), request.getStatus(), request.getReason());

        return UserResponse.fromEntity(targetUser);
    }

    /**
     * Cập nhật vai trò người dùng (CUSTOMER, SELLER, SHIPPER, ADMIN).
     */
    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Không cho phép Admin tự thay đổi vai trò của chính mình
        if (targetUser.getId().equals(admin.getId())) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_SELF_ROLE);
        }

        // 2. Không cho phép tước/hạ vai trò ADMIN của tài khoản khác
        if (targetUser.getRole() == UserRole.ADMIN && request.getRole() != UserRole.ADMIN) {
            throw new AppException(ErrorCode.CANNOT_DEMOTE_ADMIN);
        }

        // 3. Cập nhật role
        UserRole oldRole = targetUser.getRole();
        targetUser.setRole(request.getRole());
        userRepository.save(targetUser);

        // 3. Gửi thông báo in-app
        notificationService.sendNotification(
                targetUser.getId(),
                NotificationType.ORDER_STATUS_CHANGED,
                "Cập nhật vai trò tài khoản",
                "Vai trò tài khoản của bạn đã được thay đổi từ " + oldRole + " sang " + request.getRole() + " bởi Quản trị viên.",
                targetUser.getId()
        );

        log.info("[AdminUserService] Admin [{}] đã thay đổi vai trò User [{}] từ [{}] sang [{}]",
                adminEmail, targetUser.getEmail(), oldRole, request.getRole());

        return UserResponse.fromEntity(targetUser);
    }
}
