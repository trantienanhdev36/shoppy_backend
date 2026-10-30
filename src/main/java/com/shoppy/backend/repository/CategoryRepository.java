package com.shoppy.backend.repository;

import com.shoppy.backend.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Tìm danh mục theo ID đang hoạt động (chưa bị xóa) */
    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    /** Tìm danh mục theo slug (thân thiện URL) */
    Optional<Category> findBySlug(String slug);

    /** Kiểm tra slug đã tồn tại chưa */
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);

    /** Kiểm tra tên đã tồn tại chưa */
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    /** Lấy tất cả danh mục gốc (không có cha), sắp xếp theo displayOrder */
    List<Category> findByParentIsNullAndIsDeletedFalseOrderByDisplayOrderAsc();

    /** Lấy danh mục con của một danh mục cha */
    List<Category> findByParentIdAndIsDeletedFalseOrderByDisplayOrderAsc(Long parentId);

    /** Lấy tất cả danh mục đang ACTIVE (kể cả con), sắp xếp theo displayOrder */
    @Query("SELECT c FROM Category c WHERE c.isDeleted = false AND c.status = 'ACTIVE' ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllActiveOrderByDisplayOrder();

    /** Admin: Lấy danh sách danh mục có tìm kiếm, lọc theo trạng thái và phân trang */
    @Query("""
        SELECT c FROM Category c
        WHERE c.isDeleted = false
          AND (:search IS NULL OR LOWER(c.name) LIKE :search OR LOWER(c.slug) LIKE :search)
          AND (:status IS NULL OR c.status = :status)
    """)
    Page<Category> findCategoriesByAdmin(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    List<Category> findByParentIdAndIsDeletedFalse(Long parentId);
}
