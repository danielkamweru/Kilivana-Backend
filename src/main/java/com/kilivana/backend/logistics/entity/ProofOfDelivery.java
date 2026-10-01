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

    /**
     * Optional. The delivery OTP is the authoritative confirmation of hand-over, so a driver
     * who cannot capture a signature must still be able to file a proof. These were all
     * NOT NULL, which rejected every proof that omitted a signature and made the OTP flow
     * impossible to complete.
     */
    private String signatureUrl;

    private String photoUrl;

    private String otpReference;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime deliveredAt;
}
