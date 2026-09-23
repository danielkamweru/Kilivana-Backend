package com.kilivana.backend.ecommerce.repository;

import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findByReference(String reference);
    
    List<Payment> findByStatus(PaymentStatus status);
}
