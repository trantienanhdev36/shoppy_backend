package com.shoppy.backend.common.util;

import com.shoppy.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker chạy ngầm, quét Redis Queue mỗi 500ms để xử lý đơn hàng tuần tự.
 * Mỗi lần lấy ra 1 orderId → delegate xử lý cho OrderService.processOrder().
 *
 * Ưu điểm của mô hình này:
 * - API phản hồi cực nhanh (chỉ push queue, không xử lý ngay)
 * - Xử lý tuần tự, tránh race condition (mỗi đơn được xử lý một lần)
 * - Nếu server restart, đơn hàng vẫn còn trong DB với trạng thái PENDING
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueueWorker {

    private final StringRedisTemplate redisTemplate;
    private final OrderService orderService;

    /**
     * Quét Redis Queue mỗi 500ms.
     * fixedDelay đảm bảo lần chạy tiếp theo bắt đầu SAU KHI lần hiện tại kết thúc
     * (tránh chồng chéo nếu xử lý lâu hơn 500ms).
     */
    @Scheduled(fixedDelay = 500)
    public void consumeQueue() {
        // LPOP: Lấy phần tử đầu tiên ra khỏi Redis List (FIFO)
        String orderIdStr = redisTemplate.opsForList().leftPop(OrderService.ORDER_QUEUE_KEY);

        if (orderIdStr != null) {
            Long orderId = Long.parseLong(orderIdStr);
            log.info("[OrderQueueWorker] Lấy orderId [{}] từ Redis Queue để xử lý...", orderId);

            try {
                orderService.processOrder(orderId);
            } catch (Exception e) {
                // Ghi log lỗi nhưng KHÔNG đẩy lại vào queue để tránh vòng lặp vô tận
                // Đơn sẽ ở trạng thái PENDING hoặc CANCELLED tùy vào nơi lỗi xảy ra
                log.error("[OrderQueueWorker] Lỗi khi xử lý orderId [{}]: {}", orderId, e.getMessage());
            }
        }
    }
}
