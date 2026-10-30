package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.domain.request.shipping.CreateShippingCompanyRequest;
import com.shoppy.backend.domain.request.shipping.UpdateShippingCompanyRequest;
import com.shoppy.backend.domain.request.shipping.UpdateShippingCompanyStatusRequest;
import com.shoppy.backend.domain.response.shipping.ShippingCompanyResponse;
import com.shoppy.backend.service.ShippingCompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/shipping-companies")
@RequiredArgsConstructor
public class AdminShippingCompanyController {

    private final ShippingCompanyService shippingCompanyService;

    /**
     * GET /admin/shipping-companies?search=...&status=...&page=0&size=10
     * Admin: Xem danh sách đơn vị vận chuyển có tìm kiếm, lọc trạng thái và phân trang.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ShippingCompanyResponse>>> getAllShippingCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        PagedResponse<ShippingCompanyResponse> response = shippingCompanyService.getAllShippingCompaniesPaged(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn vị vận chuyển thành công!", response));
    }

    /**
     * GET /admin/shipping-companies/{id}
     * Admin: Xem chi tiết một đơn vị vận chuyển.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> getShippingCompanyById(@PathVariable Long id) {
        ShippingCompanyResponse response = shippingCompanyService.getShippingCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đơn vị vận chuyển thành công!", response));
    }

    /**
     * POST /admin/shipping-companies (multipart/form-data)
     * Admin: Thêm mới đơn vị vận chuyển kèm upload logo (Body: form-data).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> createShippingCompanyMultipart(
            @Valid @ModelAttribute CreateShippingCompanyRequest request,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) {
        ShippingCompanyResponse response = shippingCompanyService.createShippingCompany(request, logo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm đơn vị vận chuyển thành công!", response));
    }

    /**
     * POST /admin/shipping-companies (application/json)
     * Admin: Thêm mới đơn vị vận chuyển không upload ảnh logo (Body: raw JSON).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> createShippingCompanyJson(
            @Valid @RequestBody CreateShippingCompanyRequest request
    ) {
        ShippingCompanyResponse response = shippingCompanyService.createShippingCompany(request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm đơn vị vận chuyển thành công!", response));
    }

    /**
     * PUT /admin/shipping-companies/{id} (multipart/form-data)
     * Admin: Cập nhật đơn vị vận chuyển kèm thay logo mới (Body: form-data).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> updateShippingCompanyMultipart(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateShippingCompanyRequest request,
            @RequestParam(value = "logo", required = false) MultipartFile logo
    ) {
        ShippingCompanyResponse response = shippingCompanyService.updateShippingCompany(id, request, logo);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn vị vận chuyển thành công!", response));
    }

    /**
     * PUT /admin/shipping-companies/{id} (application/json)
     * Admin: Cập nhật thông tin đơn vị vận chuyển không đổi logo (Body: raw JSON).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> updateShippingCompanyJson(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShippingCompanyRequest request
    ) {
        ShippingCompanyResponse response = shippingCompanyService.updateShippingCompany(id, request, null);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn vị vận chuyển thành công!", response));
    }

    /**
     * PATCH /admin/shipping-companies/{id}/status
     * Admin: Chuyển đổi trạng thái đơn vị vận chuyển (ACTIVE, INACTIVE).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ShippingCompanyResponse>> updateShippingCompanyStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShippingCompanyStatusRequest request
    ) {
        ShippingCompanyResponse response = shippingCompanyService.updateShippingCompanyStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn vị vận chuyển thành công!", response));
    }

    /**
     * DELETE /admin/shipping-companies/{id}
     * Admin: Xóa đơn vị vận chuyển.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShippingCompany(@PathVariable Long id) {
        shippingCompanyService.deleteShippingCompany(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đơn vị vận chuyển thành công!", null));
    }
}
