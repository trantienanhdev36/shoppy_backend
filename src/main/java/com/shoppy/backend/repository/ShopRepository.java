package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Kiểm tra xem User này đã đăng ký Shop nào chưa
    boolean existsByUserIdAndIsDeletedFalse(Long userId);

    // Kiểm tra xem trùng tên Shop không
    boolean existsByNameAndIsDeletedFalse(String name);

    // Kiểm tra xem trùng tên Shop với Shop khác không
    boolean existsByNameAndIsDeletedFalseAndIdNot(String name, Long id);

    // Tìm thông tin Shop theo id
    Optional<Shop> findByIdAndIsDeletedFalse(Long id);

    // Tìm thông tin Shop theo userId
    Optional<Shop> findByUserIdAndIsDeletedFalse(Long userId);
    Optional<Shop> findByUserId(Long userId);

    /**
     * Admin: Lấy danh sách Cửa hàng có lọc theo trạng thái và tìm kiếm tên/mô tả.
     */
    @Query("""
        SELECT s FROM Shop s
        WHERE s.isDeleted = false
          AND (:search IS NULL OR LOWER(s.name) LIKE :search
                               OR (s.description IS NOT NULL AND LOWER(s.description) LIKE :search))
          AND (:status IS NULL OR s.status = :status)
    """)
    Page<Shop> findShopsByAdmin(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );
}