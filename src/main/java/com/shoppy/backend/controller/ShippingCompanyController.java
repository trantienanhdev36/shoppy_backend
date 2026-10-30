package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.response.shipping.ShippingCompanyResponse;
import com.shoppy.backend.service.ShippingCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipping-companies")
@RequiredArgsConstructor
public class ShippingCompanyController {

    private final ShippingCompanyService shippingCompanyService;

    /**
     * GET /shipping-companies
     * Lấy danh sách tất cả các hãng vận chuyển đang hoạt động (ACTIVE) và chưa bị xóa.
     * Endpoint public (không cần đăng nhập).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingCompanyResponse>>> getActiveShippingCompanies() {
        List<ShippingCompanyResponse> responses = shippingCompanyService.getAllActiveShippingCompanies();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn vị vận chuyển thành công!", responses));
    }

    /**
     * GET /shipping-companies/{id}
     * Xem thông tin chi tiết một hãng vận chuyển.
     * Endpoint public.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> getShippingCompanyById(@PathVariable Long id) {
        ShippingCompanyResponse response = shippingCompanyService.getShippingCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn vị vận chuyển thành công!", response));
    }
}
