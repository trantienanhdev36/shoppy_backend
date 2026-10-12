package com.shoppy.backend.config;

import com.shoppy.backend.common.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j // 💡 Thêm Lombok annotation này để dùng log.error()
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Kiểm tra Header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Trích xuất Token
        jwt = authHeader.substring(7);

        try {
            // 3. Trích xuất Email (Lưu ý: kiểm tra chính xác tên hàm trong JwtUtils của bạn)
            userEmail = jwtUtils.extractEmail(jwt);

            // 4. Kiểm tra xem SecurityContext đã có Authentication chưa
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 5. Validate Token
                if (jwtUtils.isTokenValid(jwt, userDetails)) {
                    System.out.println(">>> JWT HỢP LỆ CHO USER: " + userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. Gán Authentication vào context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.warn("[JWT Filter] Token không hợp lệ cho user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // 💡 In log chi tiết ngoại lệ ra Console để debug
            log.error("[JWT Filter Exception] Xác thực thất bại: {}", e.getMessage());
        }

        // 7. Cho request đi tiếp
        filterChain.doFilter(request, response);
    }
}