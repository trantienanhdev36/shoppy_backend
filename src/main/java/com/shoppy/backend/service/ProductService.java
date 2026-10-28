package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.product.CreateProductRequest;
import com.shoppy.backend.domain.request.product.ProductVariantRequest;
import com.shoppy.backend.domain.response.product.*;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.ProductRepository;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ProductResponseDTO createProduct(String email, CreateProductRequest request, List<MultipartFile> images, List<MultipartFile> variantImages) {
        // 1. Xác thực Shop
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa đăng ký hoặc sở hữu cửa hàng nào!"));

        // 2. Khởi tạo Product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShop(shop);

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
     * Lấy danh sách sản phẩm có phân trang.
     * Mặc định 5 bản ghi mỗi trang, sắp xếp theo createdAt giảm dần (mới nhất lên đầu).
     *
     * @param page  Số trang (0-indexed, mặc định 0)
     * @param size  Số bản ghi mỗi trang (mặc định 5)
     * @return PagedResponse chứa danh sách sản phẩm và metadata phân trang
     */
    public PagedResponse<ProductResponseDTO> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> productPage = productRepository.findAll(pageable);

        // Chỉ lấy price range của các sản phẩm trong trang hiện tại — tối ưu hiệu năng
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
    public ProductDetailResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        return ProductDetailResponseDTO.fromEntity(product);
    }
}