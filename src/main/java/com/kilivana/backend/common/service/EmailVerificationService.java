package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.common.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Limits commit before account transactions: concurrent callers never hold two pool connections. */
@Service @RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailOtpRequestLimiter limiter;
    private final EmailVerificationTransactions transactions;
    private final EmailOtpDispatchQueue dispatchQueue;

    /** Account-creation controllers acquire the send budget before starting the registration transaction. */
    public EmailOtpChallengeResponse startForNewAccount(User user) { return transactions.startForNewAccount(user); }

    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public EmailOtpChallengeResponse request(String email) {
        var id=limiter.acquireSend(email);
        dispatchQueue.enqueue(email,id);
        return EmailOtpChallengeResponse.accepted(id);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void verify(EmailOtpVerifyRequest request) {
        limiter.acquireVerification();
        transactions.verify(request);
    }
}
