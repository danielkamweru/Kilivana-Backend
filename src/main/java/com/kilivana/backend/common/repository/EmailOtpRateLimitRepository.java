package com.kilivana.backend.common.repository;

import com.kilivana.backend.common.entity.EmailOtpRateLimit;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface EmailOtpRateLimitRepository extends JpaRepository<EmailOtpRateLimit,String> {
    @Modifying
    @Query(value="INSERT INTO email_otp_rate_limits (bucket_key,reset_at,requests) VALUES (:key,:resetAt,0) ON CONFLICT (bucket_key) DO NOTHING",nativeQuery=true)
    void ensureBucket(@Param("key") String key,@Param("resetAt") Instant resetAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from EmailOtpRateLimit b where b.bucketKey=:key")
    Optional<EmailOtpRateLimit> lockBucket(@Param("key") String key);
    boolean existsByBucketKeyAndLatestChallengeId(String bucketKey, java.util.UUID latestChallengeId);
}
