package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT variant.product.id AS productId,
                   MIN(variant.price) AS minPrice,
                   MAX(variant.price) AS maxPrice
            FROM ProductVariant variant
            GROUP BY variant.product.id
            """)
    List<ProductPriceRange> findProductPriceRanges();

    interface ProductPriceRange {
        Long getProductId();
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
    }
}