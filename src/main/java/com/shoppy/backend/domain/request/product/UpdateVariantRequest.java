package com.shoppy.backend.domain.request.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO để cập nhật thông tin một biến thể sản phẩm.
 * Tất cả các trường đều optional — chỉ trường nào có giá trị mới được cập nhật (Partial Update).
 *
 * Dùng cho:
 *  - PUT /products/variants/{id}  (JSON — không kèm upload ảnh)
 *  - PUT /products/variants/{id}  (Multipart — kèm upload file ảnh mới)
 */
@Getter
@Setter
public class UpdateVariantRequest {

    /** Tên biến thể (ví dụ: "Đỏ - XL", "128GB"). Null = không thay đổi. */
    private String variantName;

    /** Giá bán. Phải >= 0 nếu được truyền. Null = không thay đổi. */
    @DecimalMin(value = "0", message = "Giá biến thể phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    /** Số lượng tồn kho. Phải >= 0 nếu được truyền. Null = không thay đổi. */
    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stockQuantity;

    /** Mã SKU. Null = không thay đổi. */
    private String sku;

    /**
     * URL ảnh biến thể (nếu không upload file mới mà dùng URL có sẵn).
     * Null hoặc rỗng = không thay đổi ảnh.
     * Bị bỏ qua nếu có file ảnh upload kèm theo (Multipart).
     */
    private String imageUrl;
}

