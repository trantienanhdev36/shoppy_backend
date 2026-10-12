package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.domain.request.user.DepositRequest;
import com.shoppy.backend.domain.response.user.UserResponse;
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
                ApiResponse.success("Nạp tiền mô phỏng thành công!", result)
        );
    }
}