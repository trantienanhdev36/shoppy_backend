package com.shoppy.backend.domain.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private Long categoryId;

    // Dùng cho trường hợp sản phẩm đơn (không có biến thể)
    private BigDecimal basePrice;
    private Integer baseStock;

    // Danh sách ID các ảnh cũ muốn xóa bỏ
    private List<Long> deleteImageIds = new ArrayList<>();

    // Danh sách biến thể cập nhật (nếu sản phẩm có phân loại hàng)
    @Valid
    private List<UpdateProductVariantRequest> variants = new ArrayList<>();
}
