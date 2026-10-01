package com.shoppy.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    // 1. BẢO MẬT: Nhận mật khẩu từ Request gửi lên, nhưng KHÔNG BAO GIỜ render ra JSON Response
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "avatar_url")
    private String avatarUrl;

    // 🔗 2. Tránh lỗi LazyInitializationException & Lặp vô tận khi serialize JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_company_id")
    @JsonIgnore
    private ShippingCompany shippingCompany;

    // ⚡ 3. Getter ảo: Tự động bổ sung trường shippingCompanyId vào JSON trả về cho App
    @JsonProperty("shippingCompanyId")
    public Long getShippingCompanyId() {
        return shippingCompany != null ? shippingCompany.getId() : null;
    }
}