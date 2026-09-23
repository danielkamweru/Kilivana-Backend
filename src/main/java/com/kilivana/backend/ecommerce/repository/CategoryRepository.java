package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.common.enums.SellerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findByType(SellerType type);
    
    List<Category> findByActiveTrue();
    
    List<Category> findByTypeAndActiveTrue(SellerType type);
    
    boolean existsByName(String name);
}
