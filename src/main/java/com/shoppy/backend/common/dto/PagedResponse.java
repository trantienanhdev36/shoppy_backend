package com.shoppy.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper chuẩn cho response có phân trang.
 * Dùng thay thế cho List<T> trực tiếp khi endpoint hỗ trợ pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    /** Danh sách dữ liệu của trang hiện tại */
    private List<T> content;

    /** Trang hiện tại (0-indexed) */
    private int page;

    /** Số bản ghi mỗi trang */
    private int size;

    /** Tổng số bản ghi */
    private long totalElements;

    /** Tổng số trang */
    private int totalPages;

    /** Có trang tiếp theo không */
    private boolean hasNext;

    /** Có trang trước không */
    private boolean hasPrevious;
}
