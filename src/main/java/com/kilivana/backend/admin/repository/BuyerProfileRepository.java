package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.BuyerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Access to the {@link BuyerProfile} table. A buyer profile is optional — a registered buyer may
 * never have onboarded one — so reads return {@link Optional} rather than assuming the row exists.
 */
@Repository
public interface BuyerProfileRepository extends JpaRepository<BuyerProfile, Long> {
    
    Optional<BuyerProfile> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
