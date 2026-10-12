package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Kiểm tra xem User này đã đăng ký Shop nào chưa
    boolean existsByUserIdAndIsDeletedFalse(Long userId);

    // Kiểm tra xem trùng tên Shop không
    boolean existsByNameAndIsDeletedFalse(String name);

    // Tìm thông tin Shop theo userId
    Optional<Shop> findByUserIdAndIsDeletedFalse(Long userId);
    Optional<Shop> findByUserId(Long userId);
}