package com.kilivana.backend.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proof_of_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long logisticsJobId;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String signatureUrl;

    @Column(nullable = false)
    private String photoUrl;

    @Column(nullable = false)
    private String otpReference;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime deliveredAt;
}
