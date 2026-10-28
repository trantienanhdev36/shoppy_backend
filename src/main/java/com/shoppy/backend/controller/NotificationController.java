package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.response.notification.NotificationResponse;
import com.shoppy.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /notifications?page=0&size=10
     * Lấy danh sách thông báo của người dùng đăng nhập (có phân trang).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        PagedResponse<NotificationResponse> response = notificationService.getMyNotifications(email, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công!", response));
    }

    /**
     * GET /notifications/unread-count
     * Lấy số lượng thông báo chưa đọc của người dùng.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        String email = authentication.getName();
        long unreadCount = notificationService.getUnreadCount(email);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công!", unreadCount));
    }

    /**
     * PATCH /notifications/{id}/read
     * Đánh dấu 1 thông báo cụ thể là đã đọc.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        notificationService.markAsRead(email, id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu thông báo đã đọc thành công!"));
    }

    /**
     * PATCH /notifications/read-all
     * Đánh dấu toàn bộ thông báo của người dùng là đã đọc.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        String email = authentication.getName();
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu tất cả thông báo đã đọc thành công!"));
    }
}
