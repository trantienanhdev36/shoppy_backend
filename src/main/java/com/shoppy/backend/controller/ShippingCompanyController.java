package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.exception.AppException;
import com.shoppy.backend.common.exception.ErrorCode;
import com.shoppy.backend.domain.response.shipping.ShippingCompanyResponse;
import com.shoppy.backend.entity.ShippingCompany;
import com.shoppy.backend.repository.ShippingCompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping-companies")
@RequiredArgsConstructor
public class ShippingCompanyController {

    private final ShippingCompanyRepository shippingCompanyRepository;

    /**
     * GET /shipping-companies
     * Lấy danh sách tất cả các hãng vận chuyển đang hoạt động (ACTIVE).
     * Endpoint public (không cần đăng nhập).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingCompanyResponse>>> getActiveShippingCompanies() {
        List<ShippingCompany> companies = shippingCompanyRepository.findByStatusOrderByNameAsc("ACTIVE");
        List<ShippingCompanyResponse> responses = companies.stream()
                .map(ShippingCompanyResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn vị vận chuyển thành công!", responses));
    }

    /**
     * GET /shipping-companies/{id}
     * Xem thông tin chi tiết một hãng vận chuyển.
     * Endpoint public.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> getShippingCompanyById(@PathVariable Long id) {
        ShippingCompany company = shippingCompanyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_COMPANY_NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn vị vận chuyển thành công!",
                ShippingCompanyResponse.fromEntity(company)));
    }
}
