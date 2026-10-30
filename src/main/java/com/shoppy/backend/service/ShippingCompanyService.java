package com.shoppy.backend.service;

import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.common.util.PaginationUtils;
import com.shoppy.backend.domain.request.shipping.CreateShippingCompanyRequest;
import com.shoppy.backend.domain.request.shipping.UpdateShippingCompanyRequest;
import com.shoppy.backend.domain.request.shipping.UpdateShippingCompanyStatusRequest;
import com.shoppy.backend.domain.response.shipping.ShippingCompanyResponse;
import com.shoppy.backend.entity.ShippingCompany;
import com.shoppy.backend.repository.ShippingCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingCompanyService {

    private final ShippingCompanyRepository shippingCompanyRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Public: Lấy danh sách các đơn vị vận chuyển đang hoạt động (ACTIVE).
     */
    @Transactional(readOnly = true)
    public List<ShippingCompanyResponse> getAllActiveShippingCompanies() {
        return shippingCompanyRepository.findByStatusAndIsDeletedFalseOrderByNameAsc("ACTIVE")
                .stream()
                .map(ShippingCompanyResponse::fromEntity)
                .toList();
    }

    /**
     * Lấy chi tiết đơn vị vận chuyển theo ID.
     */
    @Transactional(readOnly = true)
    public ShippingCompanyResponse getShippingCompanyById(Long id) {
        ShippingCompany company = shippingCompanyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));
        return ShippingCompanyResponse.fromEntity(company);
    }

    /**
     * Admin: Lấy danh sách tất cả đơn vị vận chuyển có tìm kiếm, lọc theo trạng thái và phân trang.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ShippingCompanyResponse> getAllShippingCompaniesPaged(
            String search,
            String status,
            int page,
            int size
    ) {
        String cleanSearch = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : null;

        return PaginationUtils.fetchWithClamp(
                page,
                size,
                "createdAt",
                Sort.Direction.DESC,
                pageable -> shippingCompanyRepository.findShippingCompaniesByAdmin(cleanSearch, cleanStatus, pageable),
                ShippingCompanyResponse::fromEntity
        );
    }

    /**
     * Admin: Thêm mới đơn vị vận chuyển (hỗ trợ upload ảnh logo).
     */
    @Transactional
    public ShippingCompanyResponse createShippingCompany(CreateShippingCompanyRequest request, MultipartFile logo) {
        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanName = request.getName().trim();

        // 1. Kiểm tra trùng mã code
        if (shippingCompanyRepository.existsByCodeAndIsDeletedFalse(cleanCode)) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_CODE_EXISTED);
        }

        // 2. Kiểm tra trùng tên
        if (shippingCompanyRepository.existsByNameAndIsDeletedFalse(cleanName)) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_NAME_EXISTED);
        }

        // 3. Chuẩn hóa & kiểm tra trạng thái
        String status = (request.getStatus() != null && !request.getStatus().trim().isEmpty())
                ? request.getStatus().trim().toUpperCase()
                : "ACTIVE";

        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new AppException(ErrorCode.INVALID_SHIPPING_COMPANY_STATUS);
        }

        // 4. Upload logo lên Cloudinary nếu có
        String logoUrl = null;
        if (logo != null && !logo.isEmpty()) {
            logoUrl = cloudinaryService.uploadImage(logo, "shoppy_shipping_logos");
        }

        // 5. Lưu entity
        ShippingCompany company = ShippingCompany.builder()
                .name(cleanName)
                .code(cleanCode)
                .baseFee(request.getBaseFee())
                .logoUrl(logoUrl)
                .status(status)
                .build();

        ShippingCompany saved = shippingCompanyRepository.save(company);
        log.info("[ShippingCompanyService] Admin đã thêm đơn vị vận chuyển mới [{}] - [{}]", saved.getId(), saved.getName());

        return ShippingCompanyResponse.fromEntity(saved);
    }

    /**
     * Admin: Cập nhật thông tin đơn vị vận chuyển (hỗ trợ đổi logo).
     */
    @Transactional
    public ShippingCompanyResponse updateShippingCompany(Long id, UpdateShippingCompanyRequest request, MultipartFile logo) {
        ShippingCompany company = shippingCompanyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        String cleanCode = request.getCode().trim().toUpperCase();
        String cleanName = request.getName().trim();

        // 1. Kiểm tra trùng mã code với công ty khác
        if (shippingCompanyRepository.existsByCodeAndIdNotAndIsDeletedFalse(cleanCode, id)) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_CODE_EXISTED);
        }

        // 2. Kiểm tra trùng tên với công ty khác
        if (shippingCompanyRepository.existsByNameAndIdNotAndIsDeletedFalse(cleanName, id)) {
            throw new AppException(ErrorCode.SHIPPING_COMPANY_NAME_EXISTED);
        }

        // 3. Chuẩn hóa & kiểm tra status
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase();
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                throw new AppException(ErrorCode.INVALID_SHIPPING_COMPANY_STATUS);
            }
            company.setStatus(status);
        }

        company.setName(cleanName);
        company.setCode(cleanCode);
        company.setBaseFee(request.getBaseFee());

        // 4. Upload logo mới nếu có
        if (logo != null && !logo.isEmpty()) {
            String newLogoUrl = cloudinaryService.uploadImage(logo, "shoppy_shipping_logos");
            company.setLogoUrl(newLogoUrl);
        }

        ShippingCompany updated = shippingCompanyRepository.save(company);
        log.info("[ShippingCompanyService] Admin đã cập nhật đơn vị vận chuyển [{}] - [{}]", updated.getId(), updated.getName());

        return ShippingCompanyResponse.fromEntity(updated);
    }

    /**
     * Admin: Chuyển đổi trạng thái đơn vị vận chuyển (ACTIVE <-> INACTIVE).
     */
    @Transactional
    public ShippingCompanyResponse updateShippingCompanyStatus(Long id, UpdateShippingCompanyStatusRequest request) {
        ShippingCompany company = shippingCompanyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        String newStatus = request.getStatus().trim().toUpperCase();
        if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) {
            throw new AppException(ErrorCode.INVALID_SHIPPING_COMPANY_STATUS);
        }

        company.setStatus(newStatus);
        ShippingCompany updated = shippingCompanyRepository.save(company);
        log.info("[ShippingCompanyService] Admin đã chuyển trạng thái đơn vị vận chuyển [{}] sang [{}]", updated.getId(), newStatus);

        return ShippingCompanyResponse.fromEntity(updated);
    }

    /**
     * Admin: Xóa mềm đơn vị vận chuyển.
     */
    @Transactional
    public void deleteShippingCompany(Long id) {
        ShippingCompany company = shippingCompanyRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));

        company.setIsDeleted(true);
        company.setStatus("INACTIVE");
        shippingCompanyRepository.save(company);
        log.info("[ShippingCompanyService] Admin đã xóa đơn vị vận chuyển [{}] - [{}]", id, company.getName());
    }
}
