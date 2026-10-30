package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.StockStatus;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.product.CreateProductRequest;
import com.shoppy.backend.domain.request.product.ProductVariantRequest;
import com.shoppy.backend.domain.request.product.AddProductVariantRequest;
import com.shoppy.backend.domain.request.product.UpdateProductRequest;
import com.shoppy.backend.domain.request.product.UpdateProductVariantRequest;
import com.shoppy.backend.domain.request.product.UpdateVariantStockRequest;
import com.shoppy.backend.domain.response.product.*;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.CategoryRepository;
import com.shoppy.backend.repository.ProductRepository;
import com.shoppy.backend.repository.ProductVariantRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ProductResponseDTO createProduct(String email, CreateProductRequest request, List<MultipartFile> images, List<MultipartFile> variantImages) {
        // 1. Xác thực Shop
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // 2. Xác thực Danh mục (Category)
        Category category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 3. Khởi tạo Product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShop(shop);
        product.setCategory(category);

        // 3. Upload ảnh chung của sản phẩm lên Cloudinary
        List<ProductImage> productImages = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile img : images) {
                if (!img.isEmpty()) {
                    String url = cloudinaryService.uploadImage(img, "shoppy_products");
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImageUrl(url);
                    productImages.add(productImage);
                }
            }
        }
        product.setImages(productImages);

        // 4. Xử lý phân loại: Có biến thể hay không có biến thể (Chuẩn Shopee)
        List<ProductVariant> variants = new ArrayList<>();
        boolean hasVariants = request.getVariants() != null && !request.getVariants().isEmpty();
        product.setHasVariants(hasVariants);

        if (!hasVariants) {
            // Trường hợp 1: Sản phẩm đơn (Không có biến thể) -> Tự động tạo 1 biến thể ngầm "Mặc định"
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(product);
            defaultVariant.setVariantName("Mặc định");
            defaultVariant.setPrice(request.getBasePrice());
            defaultVariant.setStockQuantity(request.getBaseStock());

            // Lấy ảnh đầu tiên của sản phẩm làm ảnh biến thể mặc định nếu có
            if (!productImages.isEmpty()) {
                defaultVariant.setImageUrl(productImages.get(0).getImageUrl());
            }
            variants.add(defaultVariant);
        } else {
            // Trường hợp 2: Sản phẩm có nhiều biến thể (Phân loại hàng)
            List<ProductVariantRequest> variantRequests = request.getVariants();
            for (int i = 0; i < variantRequests.size(); i++) {
                ProductVariantRequest dto = variantRequests.get(i);
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setVariantName(dto.getVariantName());
                variant.setPrice(dto.getPrice());
                variant.setStockQuantity(dto.getStockQuantity());
                variant.setSku(dto.getSku());

                // Gắn ảnh riêng cho biến thể nếu được truyền lên tương ứng theo index
                if (variantImages != null && variantImages.size() > i && variantImages.get(i) != null && !variantImages.get(i).isEmpty()) {
                    String variantImgUrl = cloudinaryService.uploadImage(variantImages.get(i), "shoppy_variants");
                    variant.setImageUrl(variantImgUrl);
                }

                variants.add(variant);
            }
        }
        product.setVariants(variants);

        // 5. Lưu vào Database
        Product savedProduct = productRepository.save(product);

        // 6. Map sang Response
        return ProductResponseDTO.fromEntity(savedProduct);
    }

    /**
     * Cập nhật thông tin sản phẩm, danh mục, giá, tồn kho, ảnh và biến thể.
     * Bắt buộc người thực hiện phải là chủ sở hữu của Shop (hoặc ADMIN).
     *
     * @param email         Email của seller thực hiện cập nhật
     * @param productId     ID của sản phẩm cần cập nhật
     * @param request       DTO chứa thông tin cập nhật
     * @param images        Danh sách ảnh sản phẩm chung mới cần thêm (optional)
     * @param variantImages Danh sách ảnh mới cho các biến thể (optional)
     * @return              Chi tiết sản phẩm sau khi cập nhật
     */
    @Transactional
    public ProductDetailResponseDTO updateProduct(
            String email,
            Long productId,
            UpdateProductRequest request,
            List<MultipartFile> images,
            List<MultipartFile> variantImages) {

        // 1. Xác thực người dùng
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Lấy sản phẩm và kiểm tra tồn tại
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 3. Kiểm tra quyền sở hữu (nếu không phải ADMIN thì phải thuộc Shop của seller)
        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        // 4. Cập nhật Category nếu có
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findByIdAndIsDeletedFalse(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        // 5. Cập nhật thông tin cơ bản
        product.setName(request.getName());
        product.setDescription(request.getDescription());

        // 6. Xử lý ảnh sản phẩm chung
        // 6.1 Xóa các ảnh được chỉ định
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            product.getImages().removeIf(img -> request.getDeleteImageIds().contains(img.getId()));
        }

        // 6.2 Upload thêm ảnh mới
        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    String url = cloudinaryService.uploadImage(file, "shoppy_products");
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImageUrl(url);
                    product.getImages().add(productImage);
                }
            }
        }

        // 7. Xử lý Biến thể (Variants), Giá & Tồn kho
        boolean hasVariants = request.getVariants() != null && !request.getVariants().isEmpty();
        product.setHasVariants(hasVariants);

        if (!hasVariants) {
            // Trường hợp: Sản phẩm đơn (không có biến thể)
            BigDecimal basePrice = request.getBasePrice() != null ? request.getBasePrice() : BigDecimal.ZERO;
            Integer baseStock = request.getBaseStock() != null ? request.getBaseStock() : 0;

            if (product.getVariants() == null || product.getVariants().isEmpty()) {
                ProductVariant defaultVariant = new ProductVariant();
                defaultVariant.setProduct(product);
                defaultVariant.setVariantName("Mặc định");
                defaultVariant.setPrice(basePrice);
                defaultVariant.setStockQuantity(baseStock);
                if (!product.getImages().isEmpty()) {
                    defaultVariant.setImageUrl(product.getImages().get(0).getImageUrl());
                }
                product.getVariants().add(defaultVariant);
            } else {
                ProductVariant defaultVariant = product.getVariants().get(0);
                defaultVariant.setVariantName("Mặc định");
                defaultVariant.setPrice(basePrice);
                defaultVariant.setStockQuantity(baseStock);
                if (!product.getImages().isEmpty() && defaultVariant.getImageUrl() == null) {
                    defaultVariant.setImageUrl(product.getImages().get(0).getImageUrl());
                }

                // Xóa các biến thể thừa nếu trước đó từng có nhiều biến thể
                product.getVariants().removeIf(v -> v != defaultVariant);
            }
        } else {
            // Trường hợp: Sản phẩm có biến thể
            Map<Long, ProductVariant> existingVariantsMap = product.getVariants().stream()
                    .filter(v -> v.getId() != null)
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

            Set<Long> retainedVariantIds = new HashSet<>();
            List<ProductVariant> newVariantsToAdd = new ArrayList<>();

            List<UpdateProductVariantRequest> variantRequests = request.getVariants();
            for (int i = 0; i < variantRequests.size(); i++) {
                UpdateProductVariantRequest dto = variantRequests.get(i);

                ProductVariant variant;
                if (dto.getId() != null && existingVariantsMap.containsKey(dto.getId())) {
                    variant = existingVariantsMap.get(dto.getId());
                    retainedVariantIds.add(dto.getId());
                } else {
                    variant = new ProductVariant();
                    variant.setProduct(product);
                }

                variant.setVariantName(dto.getVariantName());
                variant.setPrice(dto.getPrice());
                variant.setStockQuantity(dto.getStockQuantity());
                variant.setSku(dto.getSku());

                // Nếu có upload ảnh mới cho biến thể ở index này -> cập nhật ảnh
                if (variantImages != null && variantImages.size() > i && variantImages.get(i) != null && !variantImages.get(i).isEmpty()) {
                    String variantImgUrl = cloudinaryService.uploadImage(variantImages.get(i), "shoppy_variants");
                    variant.setImageUrl(variantImgUrl);
                } else if (dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty()) {
                    variant.setImageUrl(dto.getImageUrl().trim());
                }

                if (variant.getId() == null) {
                    newVariantsToAdd.add(variant);
                }
            }

            // Xóa các biến thể cũ không còn nằm trong danh sách cập nhật
            product.getVariants().removeIf(v -> v.getId() != null && !retainedVariantIds.contains(v.getId()));

            // Thêm các biến thể mới vào danh sách
            product.getVariants().addAll(newVariantsToAdd);
        }

        // 8. Lưu vào DB
        Product savedProduct = productRepository.save(product);

        return ProductDetailResponseDTO.fromEntity(savedProduct);
    }

    /**
     * Cập nhật nhanh số lượng tồn kho của một biến thể sản phẩm.
     * Kiểm tra quyền sở hữu: Biến thể phải thuộc Shop của Seller đang đăng nhập (hoặc ADMIN).
     *
     * @param email      Email của Seller thực hiện thao tác
     * @param variantId  ID của biến thể cần cập nhật kho
     * @param request    DTO chứa số lượng tồn kho mới
     * @return           Thông tin biến thể sau khi cập nhật
     */
    @Transactional
    public ProductVariantDTO updateVariantStock(String email, Long variantId, UpdateVariantStockRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        Product product = variant.getProduct();
        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        variant.setStockQuantity(request.getStockQuantity());
        ProductVariant savedVariant = productVariantRepository.save(variant);

        return ProductVariantDTO.fromEntity(savedVariant);
    }

    /**
     * Thêm nhanh một biến thể mới vào sản phẩm đã có.
     * Tự động kích hoạt hasVariants = true cho sản phẩm nếu trước đó là sản phẩm đơn.
     *
     * @param email      Email của Seller thực hiện thao tác
     * @param productId  ID của sản phẩm cần thêm biến thể
     * @param request    DTO thông tin biến thể mới
     * @param image      File ảnh riêng của biến thể (optional)
     * @return           Thông tin biến thể vừa tạo
     */
    @Transactional
    public ProductVariantDTO addVariant(
            String email,
            Long productId,
            AddProductVariantRequest request,
            MultipartFile image) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(request.getVariantName());
        variant.setPrice(request.getPrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setSku(request.getSku());

        if (image != null && !image.isEmpty()) {
            String imgUrl = cloudinaryService.uploadImage(image, "shoppy_variants");
            variant.setImageUrl(imgUrl);
        } else if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            variant.setImageUrl(request.getImageUrl().trim());
        }

        product.setHasVariants(true);
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        product.getVariants().add(variant);
        ProductVariant savedVariant = productVariantRepository.save(variant);
        productRepository.save(product);

        return ProductVariantDTO.fromEntity(savedVariant);
    }

    /**
     * Tạm ngưng kinh doanh / ẩn một biến thể sản phẩm (deactivate).
     */
    @Transactional
    public ProductVariantDTO deactivateVariant(String email, Long variantId) {
        return updateVariantActiveStatus(email, variantId, false);
    }

    /**
     * Kích hoạt lại một biến thể sản phẩm đã bị ẩn (activate).
     */
    @Transactional
    public ProductVariantDTO activateVariant(String email, Long variantId) {
        return updateVariantActiveStatus(email, variantId, true);
    }

    /**
     * Cập nhật trạng thái hoạt động (active/inactive) của một biến thể sản phẩm.
     */
    @Transactional
    public ProductVariantDTO updateVariantActiveStatus(String email, Long variantId, boolean active) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }

        Product product = variant.getProduct();
        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        variant.setIsActive(active);
        ProductVariant savedVariant = productVariantRepository.save(variant);

        return ProductVariantDTO.fromEntity(savedVariant);
    }

    /**
     * Xóa mềm một biến thể sản phẩm (soft delete).
     * Bắt buộc sản phẩm phải còn ít nhất 1 biến thể chưa bị xóa.
     */
    @Transactional
    public void softDeleteVariant(String email, Long variantId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }

        Product product = variant.getProduct();
        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        // Kiểm tra số lượng biến thể còn lại chưa bị xóa của sản phẩm
        long remainingVariants = productVariantRepository.countByProductIdAndIsDeletedFalse(product.getId());
        if (remainingVariants <= 1) {
            throw new AppException(ErrorCode.CANNOT_DELETE_LAST_VARIANT);
        }

        variant.setIsDeleted(true);
        productVariantRepository.save(variant);
    }

    /**
     * Lấy danh sách sản phẩm có phân trang.
     * Nếu {@code page} vượt quá tổng số trang → tự động clamp về trang cuối thay vì trả về rỗng.
     *
     * @param page  Số trang (0-indexed, mặc định 0)
     * @param size  Số bản ghi mỗi trang (mặc định 5)
     * @return PagedResponse chứa danh sách sản phẩm và metadata phân trang
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponseDTO> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAllActive(pageable);

        // Clamp: nếu trang yêu cầu vượt quá tổng trang và có dữ liệu → lấy trang cuối
        if (productPage.isEmpty() && productPage.getTotalPages() > 0) {
            int lastPage = productPage.getTotalPages() - 1;
            pageable = PageRequest.of(lastPage, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            productPage = productRepository.findAllActive(pageable);
        }

        return mapToPagedProductResponse(productPage);
    }

    /**
     * Lấy danh sách sản phẩm đang hoạt động của một Shop dành cho khách xem (Public) — có phân trang.
     *
     * @param shopId ID của Cửa hàng
     * @param page   Số trang (0-indexed, mặc định 0)
     * @param size   Số bản ghi mỗi trang (mặc định 10)
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponseDTO> getPublicProductsByShop(Long shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findActiveByShopId(shopId, pageable);

        if (productPage.isEmpty() && productPage.getTotalPages() > 0) {
            int lastPage = productPage.getTotalPages() - 1;
            pageable = PageRequest.of(lastPage, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            productPage = productRepository.findActiveByShopId(shopId, pageable);
        }

        return mapToPagedProductResponse(productPage);
    }

    /**
     * Lấy danh sách sản phẩm thuộc Shop của Seller đang đăng nhập — có phân trang.
     * Hỗ trợ lọc theo trạng thái kho hàng (ALL, IN_STOCK, OUT_OF_STOCK) và từ khóa tên.
     * Áp dụng chính sách "last page clamp" khi page vượt quá tổng số trang.
     *
     * @param email        Email của seller đang đăng nhập
     * @param stockStatus  Trạng thái kho (ALL, IN_STOCK, OUT_OF_STOCK)
     * @param keyword      Từ khóa tìm kiếm theo tên sản phẩm (optional)
     * @param page         Số trang (0-indexed)
     * @param size         Số bản ghi mỗi trang
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponseDTO> getSellerProducts(
            String email, StockStatus stockStatus, String keyword, int page, int size) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        Page<Product> productPage = querySellerProducts(shop.getId(), stockStatus, cleanKeyword, pageable);

        // Clamp: nếu trang yêu cầu vượt quá tổng trang và có dữ liệu → lấy trang cuối
        if (productPage.isEmpty() && productPage.getTotalPages() > 0) {
            int lastPage = productPage.getTotalPages() - 1;
            pageable = PageRequest.of(lastPage, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            productPage = querySellerProducts(shop.getId(), stockStatus, cleanKeyword, pageable);
        }

        return mapToPagedProductResponse(productPage);
    }

    private Page<Product> querySellerProducts(Long shopId, StockStatus stockStatus, String keyword, Pageable pageable) {
        boolean hasKeyword = keyword != null;

        if (stockStatus == StockStatus.IN_STOCK) {
            return hasKeyword
                    ? productRepository.findInStockByShopIdAndKeyword(shopId, keyword, pageable)
                    : productRepository.findInStockByShopId(shopId, pageable);
        } else if (stockStatus == StockStatus.OUT_OF_STOCK) {
            return hasKeyword
                    ? productRepository.findOutOfStockByShopIdAndKeyword(shopId, keyword, pageable)
                    : productRepository.findOutOfStockByShopId(shopId, pageable);
        } else {
            return hasKeyword
                    ? productRepository.findByShopIdAndKeyword(shopId, keyword, pageable)
                    : productRepository.findByShopId(shopId, pageable);
        }
    }

    private PagedResponse<ProductResponseDTO> mapToPagedProductResponse(Page<Product> productPage) {
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        Map<Long, ProductRepository.ProductPriceRange> priceRanges =
                productIds.isEmpty()
                        ? Map.of()
                        : productRepository.findProductPriceRangesByIds(productIds).stream()
                                .collect(Collectors.toMap(
                                        ProductRepository.ProductPriceRange::getProductId,
                                        Function.identity()
                                ));

        List<ProductResponseDTO> content = productPage.getContent().stream().map(product -> {
            ProductRepository.ProductPriceRange priceRange = priceRanges.get(product.getId());
            BigDecimal minPrice = priceRange != null ? priceRange.getMinPrice() : null;
            BigDecimal maxPrice = priceRange != null ? priceRange.getMaxPrice() : null;
            return ProductResponseDTO.fromEntity(product, minPrice, maxPrice);
        }).toList();

        return PagedResponse.<ProductResponseDTO>builder()
                .content(content)
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();
    }

    /**
     * Lấy chi tiết sản phẩm: Nếu có biến thể thì trả về danh sách biến thể, nếu không có thì trả về ảnh gốc.
     */
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // Kiểm tra Cửa hàng sở hữu: nếu Shop bị khóa hoặc tạm ngưng thì không hiển thị cho khách
        if (product.getShop() != null) {
            if (Boolean.TRUE.equals(product.getShop().getIsDeleted())
                    || "INACTIVE".equalsIgnoreCase(product.getShop().getStatus())
                    || "BLOCKED".equalsIgnoreCase(product.getShop().getStatus())) {
                throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
            }
        }

        // Kiểm tra Danh mục: nếu danh mục bị deactive (INACTIVE) thì ẩn sản phẩm
        if (product.getCategory() != null) {
            if (Boolean.TRUE.equals(product.getCategory().getIsDeleted())
                    || "INACTIVE".equalsIgnoreCase(product.getCategory().getStatus())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_ACTIVE);
            }
        }

        return ProductDetailResponseDTO.fromEntity(product);
    }

    /**
     * Lấy danh sách các biến thể của một sản phẩm.
     *
     * @param productId ID của sản phẩm
     * @return Danh sách ProductVariantDTO
     */
    @Transactional(readOnly = true)
    public List<ProductVariantDTO> getProductVariants(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (product.getShop() != null) {
            if (Boolean.TRUE.equals(product.getShop().getIsDeleted())
                    || "INACTIVE".equalsIgnoreCase(product.getShop().getStatus())
                    || "BLOCKED".equalsIgnoreCase(product.getShop().getStatus())) {
                throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
            }
        }

        if (product.getCategory() != null) {
            if (Boolean.TRUE.equals(product.getCategory().getIsDeleted())
                    || "INACTIVE".equalsIgnoreCase(product.getCategory().getStatus())) {
                throw new AppException(ErrorCode.CATEGORY_NOT_ACTIVE);
            }
        }

        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsDeletedFalseOrderByIdAsc(productId);
        return variants.stream()
                .map(ProductVariantDTO::fromEntity)
                .toList();
    }

    /**
     * Tạm ngưng kinh doanh / ẩn sản phẩm (deactivate).
     *
     * @param email      Email của Seller thực hiện thao tác
     * @param productId  ID của sản phẩm cần tạm ngưng
     * @return           Chi tiết sản phẩm sau khi cập nhật
     */
    @Transactional
    public ProductDetailResponseDTO deactivateProduct(String email, Long productId) {
        return updateProductActiveStatus(email, productId, false);
    }

    /**
     * Kích hoạt lại sản phẩm đang bị ẩn (activate).
     *
     * @param email      Email của Seller thực hiện thao tác
     * @param productId  ID của sản phẩm cần kích hoạt lại
     * @return           Chi tiết sản phẩm sau khi cập nhật
     */
    @Transactional
    public ProductDetailResponseDTO activateProduct(String email, Long productId) {
        return updateProductActiveStatus(email, productId, true);
    }

    /**
     * Cập nhật trạng thái hoạt động (active/inactive) của sản phẩm.
     */
    @Transactional
    public ProductDetailResponseDTO updateProductActiveStatus(String email, Long productId, boolean active) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        product.setIsActive(active);
        Product savedProduct = productRepository.save(product);

        return ProductDetailResponseDTO.fromEntity(savedProduct);
    }

    /**
     * Xóa mềm sản phẩm (soft delete).
     * Đánh dấu isDeleted = true cho sản phẩm và tất cả biến thể liên quan.
     *
     * @param email      Email của Seller thực hiện thao tác
     * @param productId  ID của sản phẩm cần xóa mềm
     */
    @Transactional
    public void softDeleteProduct(String email, Long productId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        if (user.getRole() != UserRole.ADMIN) {
            Shop shop = shopRepository.findByUserIdAndIsDeletedFalse(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

            if (!product.getShop().getId().equals(shop.getId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }
        }

        product.setIsDeleted(true);
        if (product.getVariants() != null) {
            product.getVariants().forEach(v -> v.setIsDeleted(true));
        }

        productRepository.save(product);
    }
}