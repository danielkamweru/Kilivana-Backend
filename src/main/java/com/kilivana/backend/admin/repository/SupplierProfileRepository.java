package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.SupplierProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

/**
 * Access to the {@link SupplierProfile} table. A supplier profile is optional — a registered
 * supplier may never have onboarded one — so reads return {@link Optional}.
 */
@Repository
public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, Long> {

    Optional<SupplierProfile> findByUserId(Long userId);

    List<SupplierProfile> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserId(Long userId);
}