package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Access to the {@link Address} table. Addresses belong to one user; the owner is carried on the
 * row ({@code userId}) rather than implied by a relationship, so lookups are by user id and the
 * service layer enforces ownership on update and delete.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);
}