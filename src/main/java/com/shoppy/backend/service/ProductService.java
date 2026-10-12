package com.shoppy.backend.service;

import com.shoppy.backend.domain.request.product.CreateProductRequest;
import com.shoppy.backend.domain.request.product.ProductVariantRequest;
import com.shoppy.backend.domain.response.product.*;
import com.shoppy.backend.entity.*;
import com.shoppy.backend.repository.ProductRepository;
import com.shoppy.backend.repository.ShopRepository;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
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
        return mapToProductResponse(savedProduct);
    }

    private ProductResponseDTO mapToProductResponse(Product product) {
        ProductResponseDTO response = new ProductResponseDTO();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setHasVariants(product.isHasVariants());

        // Map danh sách ảnh chung sang ProductImageDTO
        if (product.getImages() != null) {
            List<ProductImageDTO> imageResponses = product.getImages().stream().map(img -> {
                ProductImageDTO ir = new ProductImageDTO();
                ir.setId(img.getId());
                ir.setImageUrl(img.getImageUrl());
                return ir;
            }).toList();
            response.setImages(imageResponses);
        }

        return response;
    }

    // Lấy tất cả sản phẩm gốc (Chỉ lấy thông tin sản phẩm và ảnh gốc, không trả về biến thể)
    public List<ProductResponseDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream().map(product -> {
            ProductResponseDTO dto = new ProductResponseDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setDescription(product.getDescription());
            dto.setHasVariants(product.isHasVariants());

            // Map danh sách ảnh của sản phẩm gốc
            if (product.getImages() != null) {
                List<ProductImageDTO> imageDTOs = product.getImages().stream().map(img -> {
                    ProductImageDTO imgDto = new ProductImageDTO();
                    imgDto.setId(img.getId());
                    imgDto.setImageUrl(img.getImageUrl());
                    return imgDto;
                }).collect(Collectors.toList());
                dto.setImages(imageDTOs);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    // Lấy chi tiết sản phẩm: Nếu có biến thể thì trả về danh sách biến thể, nếu không có thì trả về ảnh gốc
    public ProductDetailResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        ProductDetailResponseDTO dto = new ProductDetailResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setHasVariants(product.isHasVariants());

        // Kiểm tra nếu sản phẩm có biến thể
        if (product.isHasVariants() && product.getVariants() != null && !product.getVariants().isEmpty()) {
            List<ProductVariantDTO> variantDTOs = product.getVariants().stream().map(variant -> {
                ProductVariantDTO vDto = new ProductVariantDTO();
                vDto.setId(variant.getId());
                vDto.setSku(variant.getSku());
                vDto.setPrice(variant.getPrice());
                vDto.setStockQuantity(variant.getStockQuantity());

                // Nếu ProductVariant dùng imageUrl kiểu String đơn thuần (như logic ở hàm create)
                if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) {
                    ProductImageDTO imgDto = new ProductImageDTO();
                    // Nếu imageUrl là String, ta có thể gán trực tiếp hoặc bọc vào DTO tùy cấu trúc của bạn
                    imgDto.setImageUrl(variant.getImageUrl());
                    vDto.setImages(List.of(imgDto));
                }

                return vDto;
            }).collect(Collectors.toList());

            dto.setVariants(variantDTOs);
        } else {
            // Nếu không có biến thể, trả về ảnh của sản phẩm gốc
            if (product.getImages() != null) {
                List<ProductImageDTO> imageDTOs = product.getImages().stream().map(img -> {
                    ProductImageDTO imgDto = new ProductImageDTO();
                    imgDto.setId(img.getId());
                    imgDto.setImageUrl(img.getImageUrl());
                    return imgDto;
                }).collect(Collectors.toList());
                dto.setImages(imageDTOs);
            }
        }

        return dto;
    }
}