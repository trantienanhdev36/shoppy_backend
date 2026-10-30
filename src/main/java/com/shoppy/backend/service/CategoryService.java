package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.category.CategoryRequest;
import com.shoppy.backend.domain.request.category.UpdateCategoryStatusRequest;
import com.shoppy.backend.domain.response.category.CategoryResponse;
import com.shoppy.backend.entity.Category;
import com.shoppy.backend.repository.CategoryRepository;
import com.shoppy.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Lấy toàn bộ cây danh mục đa cấp (Public cho khách và người bán).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesTree() {
        List<Category> allCategories = categoryRepository.findAllActiveOrderByDisplayOrder();

        // Gom nhóm các danh mục con theo parentId
        Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Lấy danh mục gốc và dựng cây đệ quy
        return allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> buildCategoryTree(root, childrenMap))
                .toList();
    }

    private CategoryResponse buildCategoryTree(Category current, Map<Long, List<Category>> childrenMap) {
        CategoryResponse response = CategoryResponse.fromEntity(current);
        List<Category> children = childrenMap.getOrDefault(current.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            response.setChildren(
                    children.stream()
                            .map(child -> buildCategoryTree(child, childrenMap))
                            .toList()
            );
        }
        return response;
    }

    /**
     * Admin: Lấy danh sách danh mục phẳng có tìm kiếm, lọc theo trạng thái và phân trang.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> getAllCategoriesPaged(String search, String status, int page, int size) {
        String cleanSearch = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "displayOrder",
                Sort.Direction.ASC,
                pageable -> categoryRepository.findCategoriesByAdmin(cleanSearch, cleanStatus, pageable),
                CategoryResponse::fromEntity
        );
    }

    /**
     * Lấy chi tiết một danh mục theo ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return CategoryResponse.fromEntity(category);
    }

    /**
     * Tạo mới một danh mục sản phẩm (Admin).
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile image) {
        // 1. Kiểm tra trùng tên
        if (categoryRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
        }

        // 2. Tạo slug chuẩn
        String slug = (request.getSlug() != null && !request.getSlug().trim().isEmpty())
                ? toSlug(request.getSlug().trim())
                : toSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
        }

        // 3. Kiểm tra parent category nếu có
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        // 4. Upload ảnh nếu có
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = cloudinaryService.uploadImage(image, "shoppy_categories");
        }

        // 5. Lưu vào database
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : "ACTIVE";

        Category category = Category.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(imageUrl)
                .parent(parent)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .status(status)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("[CategoryService] Đã tạo danh mục mới: [{}] - slug: [{}]", saved.getName(), saved.getSlug());

        return CategoryResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin danh mục sản phẩm (Admin).
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, MultipartFile image) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 1. Kiểm tra trùng tên với danh mục khác
        if (!category.getName().equalsIgnoreCase(request.getName().trim())) {
            if (categoryRepository.existsByNameAndIdNot(request.getName().trim(), id)) {
                throw new AppException(ErrorCode.CATEGORY_NAME_EXISTED);
            }
            category.setName(request.getName().trim());
        }

        // 2. Kiểm tra slug
        String slug = (request.getSlug() != null && !request.getSlug().trim().isEmpty())
                ? toSlug(request.getSlug().trim())
                : toSlug(request.getName());

        if (!slug.equals(category.getSlug())) {
            if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
                throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTED);
            }
            category.setSlug(slug);
        }

        // 3. Kiểm tra parent
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new AppException(ErrorCode.CATEGORY_PARENT_INVALID);
            }
            Category parent = categoryRepository.findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

            // Kiểm tra không được chọn con cháu làm cha để tránh tạo chu trình kín
            if (isDescendant(category, parent)) {
                throw new AppException(ErrorCode.CATEGORY_PARENT_INVALID);
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        // 4. Cập nhật các trường cơ bản
        category.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            category.setStatus(request.getStatus().trim().toUpperCase());
        }

        // 5. Upload ảnh mới nếu có
        if (image != null && !image.isEmpty()) {
            String newImageUrl = cloudinaryService.uploadImage(image, "shoppy_categories");
            category.setImageUrl(newImageUrl);
        }

        Category updated = categoryRepository.save(category);
        log.info("[CategoryService] Đã cập nhật danh mục: [{}]", updated.getName());

        return CategoryResponse.fromEntity(updated);
    }

    /**
     * Cập nhật trạng thái danh mục (ACTIVE, INACTIVE).
     */
    @Transactional
    public CategoryResponse updateCategoryStatus(Long id, UpdateCategoryStatusRequest request) {
        String newStatus = request.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_STATUS);
        }

        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        category.setStatus(newStatus);
        Category saved = categoryRepository.save(category);
        log.info("[CategoryService] Đã cập nhật trạng thái danh mục [{}] thành [{}]", saved.getName(), newStatus);

        return CategoryResponse.fromEntity(saved);
    }

    /**
     * Xóa danh mục sản phẩm (Soft delete).
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 1. Kiểm tra xem danh mục có sản phẩm kinh doanh không
        if (productRepository.existsByCategoryIdAndIsDeletedFalse(id)) {
            throw new AppException(ErrorCode.CANNOT_DELETE_CATEGORY_HAS_PRODUCTS);
        }

        // 2. Nâng các danh mục con lên danh mục cha của category hiện tại
        List<Category> children = categoryRepository.findByParentIdAndIsDeletedFalseOrderByDisplayOrderAsc(id);
        for (Category child : children) {
            child.setParent(category.getParent());
            categoryRepository.save(child);
        }

        // 3. Đánh dấu xóa mềm
        category.setIsDeleted(true);
        categoryRepository.save(category);

        log.info("[CategoryService] Đã xóa mềm danh mục: [{}]", category.getName());
    }

    /** Helper kiểm tra xem targetParent có phải là con cháu của category không */
    private boolean isDescendant(Category category, Category potentialParent) {
        Category curr = potentialParent;
        while (curr != null) {
            if (curr.getId().equals(category.getId())) {
                return true;
            }
            curr = curr.getParent();
        }
        return false;
    }

    /** Helper tạo URL slug thân thiện từ chuỗi tiếng Việt */
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noDiacritics = pattern.matcher(normalized).replaceAll("")
                .replace("đ", "d").replace("Đ", "D");
        return noDiacritics.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
