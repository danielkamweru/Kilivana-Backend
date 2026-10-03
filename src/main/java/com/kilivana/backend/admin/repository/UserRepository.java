package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.common.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Email lookups ignore case and surrounding spaces: addresses are stored lowercased, and
    // a case-sensitive comparison used to let "Jane@x.com" and "jane@x.com" register as two
    // accounts, so the second registration failed as a conflict and login failed as unknown.
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    /** Username is unique when present, so a duplicate has to be caught case-insensitively. */
    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByUsernameIgnoreCase(String username);
    
    List<User> findByRole(UserRole role);

    /** An order may only be placed by a buyer account. */
    boolean existsByIdAndRole(Long id, UserRole role);

    List<User> findByStatus(UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);

    long countByRole(UserRole role);

    /** Existing per-role codes such as {@code F-014}, used to hand out the next one. */
    @Query("SELECT u.referenceCode FROM User u WHERE u.role = :role AND u.referenceCode IS NOT NULL")
    List<String> findReferenceCodesByRole(UserRole role);

    long countByRoleAndVerificationStatus(UserRole role, VerificationStatus verificationStatus);

    long countByVerificationStatus(VerificationStatus verificationStatus);
}
