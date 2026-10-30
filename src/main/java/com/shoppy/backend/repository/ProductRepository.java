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
            WHERE variant.isDeleted = false
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
              AND variant.isDeleted = false
            GROUP BY variant.product.id
            """)
    List<ProductPriceRange> findProductPriceRangesByIds(@Param("productIds") List<Long> productIds);

    interface ProductPriceRange {
        Long getProductId();
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
    }

    /** Lấy tất cả sản phẩm đang hoạt động trên sàn (chưa bị xóa, đang active, Shop ACTIVE và Danh mục ACTIVE) - hỗ trợ tìm kiếm */
    @Query("""
            SELECT p FROM Product p
            WHERE p.isDeleted = false
              AND p.isActive = true
              AND p.shop.isDeleted = false
              AND p.shop.status = 'ACTIVE'
              AND (p.category IS NULL OR (p.category.isDeleted = false AND p.category.status = 'ACTIVE'))
              AND (:search IS NULL OR LOWER(p.name) LIKE :search)
            """)
    Page<Product> findAllActive(@Param("search") String search, Pageable pageable);

    default Page<Product> findAllActive(Pageable pageable) {
        return findAllActive(null, pageable);
    }

    /** Lấy tất cả sản phẩm của Shop (chưa bị xóa) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
            """)
    Page<Product> findByShopId(@Param("shopId") Long shopId, Pageable pageable);

    /** Lấy các sản phẩm đang hoạt động của Shop dành cho khách xem (chưa bị xóa, đang active, Shop ACTIVE và Danh mục ACTIVE) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND p.isActive = true
              AND p.shop.isDeleted = false
              AND p.shop.status = 'ACTIVE'
              AND (p.category IS NULL OR (p.category.isDeleted = false AND p.category.status = 'ACTIVE'))
            """)
    Page<Product> findActiveByShopId(@Param("shopId") Long shopId, Pageable pageable);

    /** Lấy sản phẩm của Shop theo từ khóa tìm kiếm tên */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Product> findByShopIdAndKeyword(@Param("shopId") Long shopId, @Param("keyword") String keyword, Pageable pageable);

    /** Lấy sản phẩm CÒN HÀNG của Shop (tổng tồn kho > 0) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND (SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product = p AND v.isDeleted = false) > 0
            """)
    Page<Product> findInStockByShopId(@Param("shopId") Long shopId, Pageable pageable);

    /** Lấy sản phẩm CÒN HÀNG của Shop theo từ khóa (tổng tồn kho > 0) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND (SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product = p AND v.isDeleted = false) > 0
            """)
    Page<Product> findInStockByShopIdAndKeyword(@Param("shopId") Long shopId, @Param("keyword") String keyword, Pageable pageable);

    /** Lấy sản phẩm HẾT HÀNG của Shop (tổng tồn kho <= 0) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND (SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product = p AND v.isDeleted = false) <= 0
            """)
    Page<Product> findOutOfStockByShopId(@Param("shopId") Long shopId, Pageable pageable);

    /** Lấy sản phẩm HẾT HÀNG của Shop theo từ khóa (tổng tồn kho <= 0) */
    @Query("""
            SELECT p FROM Product p
            WHERE p.shop.id = :shopId
              AND p.isDeleted = false
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              AND (SELECT COALESCE(SUM(v.stockQuantity), 0) FROM ProductVariant v WHERE v.product = p AND v.isDeleted = false) <= 0
            """)
    Page<Product> findOutOfStockByShopIdAndKeyword(@Param("shopId") Long shopId, @Param("keyword") String keyword, Pageable pageable);

    /** Đếm tổng số sản phẩm còn kinh doanh của Shop */
    long countByShopIdAndIsDeletedFalse(Long shopId);

    /** Kiểm tra xem danh mục có sản phẩm nào đang kinh doanh không */
    boolean existsByCategoryIdAndIsDeletedFalse(Long categoryId);
}