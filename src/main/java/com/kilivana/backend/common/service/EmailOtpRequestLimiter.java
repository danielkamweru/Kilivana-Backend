package com.kilivana.backend.common.service;

import com.kilivana.backend.common.repository.EmailOtpRateLimitRepository;
import com.kilivana.backend.common.exception.EmailOtpRateLimitException;
import com.kilivana.backend.security.EmailOtpSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.request.*;
import java.time.*;

@Service @RequiredArgsConstructor
public class EmailOtpRequestLimiter {
    private final EmailOtpRateLimitRepository buckets;
    private final EmailOtpSecurity security;

    /** Commits limits even if a later send or registration fails. All addresses get identical buckets. */
    @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=EmailOtpRateLimitException.class)
    public java.util.UUID acquireSend(String email) {
        consume("send-ip",clientAddress(),60,0);
        var bucket=consume("send-email",EmailOtpSecurity.normalize(email),5,60);
        var id=java.util.UUID.randomUUID();bucket.setLatestChallengeId(id);buckets.save(bucket);return id;
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW,noRollbackFor=EmailOtpRateLimitException.class)
    public void acquireVerification() { consume("verify-ip",clientAddress(),120,0); }

    private com.kilivana.backend.common.entity.EmailOtpRateLimit consume(String scope,String identity,int maximum,int cooldownSeconds) {
        Instant now=security.now();String key=security.bucketKey(scope,identity);
        buckets.ensureBucket(key,now.plusSeconds(3600));
        var bucket=buckets.lockBucket(key).orElseThrow();
        if(!bucket.getResetAt().isAfter(now)) { bucket.setRequests(0);bucket.setResetAt(now.plusSeconds(3600)); }
        if(bucket.getRequests()>=maximum) throw new EmailOtpRateLimitException(Duration.between(now,bucket.getResetAt()).getSeconds()+1);
        if(cooldownSeconds>0 && bucket.getLastRequestedAt()!=null && bucket.getLastRequestedAt().plusSeconds(cooldownSeconds).isAfter(now))
            throw new EmailOtpRateLimitException(Duration.between(now,bucket.getLastRequestedAt().plusSeconds(cooldownSeconds)).getSeconds()+1);
        bucket.setRequests(bucket.getRequests()+1);bucket.setLastRequestedAt(now);buckets.save(bucket);return bucket;
    }
    private String clientAddress() {
        var attrs=RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes servlet ? servlet.getRequest().getRemoteAddr() : "internal";
    }
}
