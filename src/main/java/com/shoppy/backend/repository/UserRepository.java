package com.shoppy.backend.repository;

import com.shoppy.backend.common.enums.UserRole;
import com.shoppy.backend.common.enums.UserStatus;
import com.shoppy.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR LOWER(u.email) LIKE :search
                               OR LOWER(u.fullName) LIKE :search
                               OR u.phone LIKE :search)
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
    """)
    Page<User> findUsersByAdmin(
            @Param("search") String search,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}