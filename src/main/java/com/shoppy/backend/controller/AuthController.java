package com.shoppy.backend.controller;

import com.shoppy.backend.common.dto.ApiResponse;
import com.shoppy.backend.dto.auth.AuthResponse;
import com.shoppy.backend.dto.auth.LoginRequest;
import com.shoppy.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        boolean isSuccess = authService.verifyAccount(token);

        if (isSuccess) {
            String successHtml = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác thực thành công - Shoppy</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .card { background: white; padding: 40px 30px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.08); text-align: center; max-width: 380px; width: 85%; }
                        .icon { width: 70px; height: 70px; background: #e6f4ea; color: #1e8e3e; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 36px; margin: 0 auto 20px; }
                        h2 { color: #202124; margin-bottom: 10px; font-size: 22px; }
                        p { color: #5f6368; font-size: 14px; line-height: 1.5; margin-bottom: 25px; }
                        .btn { display: inline-block; padding: 14px 28px; background-color: #ee4d2d; color: white; text-decoration: none; border-radius: 30px; font-weight: 600; font-size: 15px; box-shadow: 0 4px 12px rgba(238, 77, 45, 0.3); }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">✓</div>
                        <h2>Xác Thực Thành Công!</h2>
                        <p>Tài khoản Shoppy của bạn đã kích hoạt thành công. Bạn có thể đăng nhập ứng dụng ngay bây giờ.</p>
                        <a href="shoppy://login" class="btn">MỞ ỨNG DỤNG SHOPPY</a>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok(successHtml);
        } else {
            String errorHtml = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác thực thất bại - Shoppy</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                        .card { background: white; padding: 40px 30px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.08); text-align: center; max-width: 380px; width: 85%; }
                        .icon { width: 70px; height: 70px; background: #fce8e6; color: #d93025; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 36px; margin: 0 auto 20px; }
                        h2 { color: #202124; margin-bottom: 10px; font-size: 22px; }
                        p { color: #5f6368; font-size: 14px; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="icon">✕</div>
                        <h2>Xác Thực Thất Bại!</h2>
                        <p>Đường link xác thực không hợp lệ hoặc đã hết hạn (24h). Vui lòng gửi lại yêu cầu xác thực từ ứng dụng.</p>
                    </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().body(errorHtml);
        }
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    // 2. Ví dụ API Đổi mật khẩu hoặc Quên mật khẩu (Trả về thành công KHÔNG CÓ dữ liệu)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Logic logout...
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }
}