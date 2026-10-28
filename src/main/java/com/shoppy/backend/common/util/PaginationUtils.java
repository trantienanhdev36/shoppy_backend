package com.shoppy.backend.common.util;

import com.shoppy.backend.common.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class chứa các helper dùng chung cho phân trang.
 *
 * <p><b>Chính sách "last page clamp":</b> Nếu {@code page} được request vượt quá
 * tổng số trang thực tế, tự động redirect về trang cuối cùng thay vì trả về
 * danh sách rỗng. Hành vi này thân thiện hơn với client.</p>
 */
public final class PaginationUtils {

    private PaginationUtils() { /* utility class — không khởi tạo */ }

    /**
     * Giao diện functional cho một hàm query nhận Pageable và trả về Page<T>.
     */
    @FunctionalInterface
    public interface PageQuery<T> {
        Page<T> query(Pageable pageable);
    }

    /**
     * Thực hiện query có phân trang với chính sách "last page clamp":
     * <ul>
     *   <li>Nếu {@code page} hợp lệ (< totalPages) → trả kết quả bình thường.</li>
     *   <li>Nếu {@code page} >= totalPages và dữ liệu tồn tại → trả về trang cuối.</li>
     *   <li>Nếu không có dữ liệu nào → trả về trang rỗng (page=0).</li>
     * </ul>
     *
     * @param requestedPage  Số trang client yêu cầu (0-indexed)
     * @param size           Số bản ghi mỗi trang
     * @param sortField      Tên field để sort
     * @param sortDir        Hướng sort (DESC / ASC)
     * @param queryFn        Lambda thực hiện query DB với Pageable
     * @param mapper         Hàm map từ entity T sang DTO R
     * @return               PagedResponse<R> đã được clamp nếu cần
     */
    public static <T, R> PagedResponse<R> fetchWithClamp(
            int requestedPage,
            int size,
            String sortField,
            Sort.Direction sortDir,
            PageQuery<T> queryFn,
            Function<T, R> mapper
    ) {
        Pageable pageable = PageRequest.of(requestedPage, size, Sort.by(sortDir, sortField));
        Page<T> resultPage = queryFn.query(pageable);

        // Clamp: nếu trang yêu cầu vượt quá tổng trang và có dữ liệu → lấy trang cuối
        if (resultPage.isEmpty() && resultPage.getTotalPages() > 0) {
            int lastPage = resultPage.getTotalPages() - 1;
            pageable = PageRequest.of(lastPage, size, Sort.by(sortDir, sortField));
            resultPage = queryFn.query(pageable);
        }

        List<R> content = resultPage.getContent().stream().map(mapper).toList();

        return PagedResponse.<R>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .hasNext(resultPage.hasNext())
                .hasPrevious(resultPage.hasPrevious())
                .build();
    }
}
