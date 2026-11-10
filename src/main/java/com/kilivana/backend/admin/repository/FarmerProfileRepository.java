package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.FarmerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

/**
 * Access to the {@link FarmerProfile} table. A farmer profile is optional — a registered farmer
 * may never have onboarded one — so reads return {@link Optional}.
 */
@Repository
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {

    Optional<FarmerProfile> findByUserId(Long userId);

    List<FarmerProfile> findByUserIdIn(Collection<Long> userIds);

    boolean existsByUserId(Long userId);
}