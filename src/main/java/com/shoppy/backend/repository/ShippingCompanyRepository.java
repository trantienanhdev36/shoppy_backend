package com.shoppy.backend.repository;

import com.shoppy.backend.entity.ShippingCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingCompanyRepository extends JpaRepository<ShippingCompany, Long> {

    /** Lấy danh sách công ty vận chuyển theo trạng thái (ví dụ ACTIVE) sắp xếp tên A-Z */
    List<ShippingCompany> findByStatusOrderByNameAsc(String status);
}