package com.shoppy.backend.repository;

import com.shoppy.backend.entity.ShippingCompany;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingCompanyRepository extends JpaRepository<ShippingCompany, Long> {

    /** Lấy danh sách công ty vận chuyển theo trạng thái (ví dụ ACTIVE) sắp xếp tên A-Z */
    List<ShippingCompany> findByStatusOrderByNameAsc(String status);

    /** Lấy danh sách công ty vận chuyển theo trạng thái và chưa bị xóa */
    List<ShippingCompany> findByStatusAndIsDeletedFalseOrderByNameAsc(String status);

    /** Tìm công ty vận chuyển theo ID và chưa bị xóa */
    Optional<ShippingCompany> findByIdAndIsDeletedFalse(Long id);

    /** Kiểm tra mã code đã tồn tại chưa */
    boolean existsByCodeAndIsDeletedFalse(String code);

    /** Kiểm tra tên đã tồn tại chưa */
    boolean existsByNameAndIsDeletedFalse(String name);

    /** Kiểm tra mã code trùng với công ty khác không */
    boolean existsByCodeAndIdNotAndIsDeletedFalse(String code, Long id);

    /** Kiểm tra tên trùng với công ty khác không */
    boolean existsByNameAndIdNotAndIsDeletedFalse(String name, Long id);

    /**
     * Admin: Lọc và phân trang danh sách đơn vị vận chuyển theo từ khóa và trạng thái.
     */
    @Query("""
            SELECT s FROM ShippingCompany s
            WHERE s.isDeleted = false
              AND (:search IS NULL OR LOWER(s.name) LIKE :search OR LOWER(s.code) LIKE :search)
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<ShippingCompany> findShippingCompaniesByAdmin(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );
}