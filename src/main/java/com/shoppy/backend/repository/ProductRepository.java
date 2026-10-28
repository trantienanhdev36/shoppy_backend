package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Lấy toàn bộ price range (dùng khi cần lấy tất cả sản phẩm).
     */
    @Query("""
            SELECT variant.product.id AS productId,
                   MIN(variant.price) AS minPrice,
                   MAX(variant.price) AS maxPrice
            FROM ProductVariant variant
            GROUP BY variant.product.id
            """)
    List<ProductPriceRange> findProductPriceRanges();

    /**
     * Lấy price range chỉ cho các product ID thuộc trang hiện tại.
     * Tránh load toàn bộ dữ liệu khi phân trang.
     */
    @Query("""
            SELECT variant.product.id AS productId,
                   MIN(variant.price) AS minPrice,
                   MAX(variant.price) AS maxPrice
            FROM ProductVariant variant
            WHERE variant.product.id IN :productIds
            GROUP BY variant.product.id
            """)
    List<ProductPriceRange> findProductPriceRangesByIds(@Param("productIds") List<Long> productIds);

    interface ProductPriceRange {
        Long getProductId();
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
    }
}