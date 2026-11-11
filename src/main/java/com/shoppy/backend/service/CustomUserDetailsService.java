package com.shoppy.backend.service;

import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Trả về entity User trực tiếp làm UserDetails,
 * cho phép các Controller lấy userId từ Authentication.getPrincipal() mà không cần query DB thêm.
 * <p>
 * Mapping trạng thái UserStatus → Spring Security UserDetails flags:
 *  - ACTIVE   → enabled=true,  accountNonLocked=true  → JWT hợp lệ, được phép truy cập
 *  - BLOCKED  → enabled=false, accountNonLocked=false  → JWT bị vô hiệu ngay lập tức, trả 403
 *  - INACTIVE → enabled=false, accountNonLocked=true   → JWT bị vô hiệu, trả 403
 *  - PENDING  → enabled=false, accountNonLocked=true   → JWT bị vô hiệu, trả 403
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.shoppy.backend.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + email));

        boolean isActive = user.getStatus() == UserStatus.ACTIVE;
        boolean isNonLocked = user.getStatus() != UserStatus.BLOCKED;

        // Bọc entity User thành Spring Security UserDetails
        // enabled=false hoặc accountNonLocked=false sẽ khiến isTokenValid() trả false
        // → JWT token cũ bị vô hiệu ngay khi tài khoản bị thay đổi trạng thái
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!isActive)
                .accountLocked(!isNonLocked)
                .build();
    }
}