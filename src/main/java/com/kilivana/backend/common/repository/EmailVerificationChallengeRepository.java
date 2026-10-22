package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.EmailVerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationChallengeRepository extends JpaRepository<EmailVerificationChallenge,Long> {
    Optional<EmailVerificationChallenge> findByChallengeId(UUID challengeId);
}
