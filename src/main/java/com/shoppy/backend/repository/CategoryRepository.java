package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Tìm danh mục theo slug (thân thiện URL) */
    Optional<Category> findBySlug(String slug);

    /** Kiểm tra slug đã tồn tại chưa */
    boolean existsBySlug(String slug);

    /** Kiểm tra tên đã tồn tại chưa */
    boolean existsByName(String name);

    /** Lấy tất cả danh mục gốc (không có cha), sắp xếp theo displayOrder */
    List<Category> findByParentIsNullAndIsDeletedFalseOrderByDisplayOrderAsc();

    /** Lấy danh mục con của một danh mục cha */
    List<Category> findByParentIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long parentId);

    /** Lấy tất cả danh mục (kể cả con), sắp xếp theo displayOrder */
    @Query("SELECT c FROM Category c WHERE c.isDeleted = false ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllActiveOrderByDisplayOrder();
}
