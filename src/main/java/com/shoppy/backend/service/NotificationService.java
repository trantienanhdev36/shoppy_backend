package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.NotificationType;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.domain.response.notification.NotificationResponse;
import com.shoppy.backend.entity.Notification;
import com.shoppy.backend.entity.User;
import com.shoppy.backend.repository.NotificationRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Gửi thông báo bất đồng bộ tới người dùng (in-app notification).
     * Chạy trên thread riêng nhờ @Async để không làm chậm luồng nghiệp vụ chính.
     */
    @Async
    public void sendNotification(Long recipientId, NotificationType type, String title, String message, Long referenceId) {
        if (recipientId == null) {
            log.warn("[NotificationService] Bỏ qua gửi thông báo: recipientId null");
            return;
        }

        User recipient = userRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            log.warn("[NotificationService] Không tìm thấy user [{}] để gửi thông báo", recipientId);
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("[NotificationService] Đã gửi thông báo [{}] cho user [{}] (email: {})",
                type, recipient.getId(), recipient.getEmail());
    }

    /**
     * Lấy danh sách thông báo của người dùng có phân trang.
     */
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage =
                notificationRepository.findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(user.getId(), pageable);

        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(NotificationResponse::fromEntity)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    /**
     * Đếm số lượng thông báo chưa đọc.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.countByRecipientIdAndIsReadFalseAndIsDeletedFalse(user.getId());
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc.
     */
    @Transactional
    public void markAsRead(String email, Long notificationId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc.
     */
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        notificationRepository.markAllAsRead(user.getId());
    }
}
