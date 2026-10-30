package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.common.dto.PagedResponse;
import com.shoppy.backend.common.enums.WalletTransactionType;
import com.shoppy.backend.domain.request.user.DepositRequest;
import com.shoppy.backend.domain.response.user.UserResponse;
import com.shoppy.backend.domain.response.wallet.WalletTransactionResponse;
import com.shoppy.backend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<UserResponse.WalletDepositResponse>> deposit(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request
    ) {
        String email = authentication.getName();
        UserResponse.WalletDepositResponse result = walletService.createDepositRequest(email, request.getAmount());

        return ResponseEntity.ok(
                ApiResponse.success("Yêu cầu nạp tiền đã được tiếp nhận và đang xử lý.", result)
        );
    }

    /**
     * GET /users/wallet/transactions?page=0&size=10&type=CREDIT
     * Lấy lịch sử giao dịch ví của tài khoản hiện tại.
     * Hỗ trợ phân trang và lọc theo loại giao dịch (CREDIT / DEBIT).
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedResponse<WalletTransactionResponse>>> getMyWalletTransactions(
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        PagedResponse<WalletTransactionResponse> response =
                walletService.getMyWalletTransactions(email, type, page, size);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy lịch sử giao dịch ví thành công!", response)
        );
    }
}