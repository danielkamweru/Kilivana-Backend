package com.kilivana.backend.admin.repository;

import com.kilivana.backend.admin.entity.SupplierWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplierWarehouseRepository extends JpaRepository<SupplierWarehouse, Long> {
    List<SupplierWarehouse> findByUserIdOrderById(Long userId);
    List<SupplierWarehouse> findByUserIdInOrderById(Collection<Long> userIds);
    Optional<SupplierWarehouse> findByIdAndUserId(Long id, Long userId);
    Optional<SupplierWarehouse> findByUserIdAndDefaultLocationTrue(Long userId);
    boolean existsByUserIdAndDefaultLocationTrueAndIdNot(Long userId, Long id);
}
