package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.*;
import com.kilivana.backend.common.entity.EmailVerificationChallenge;
import com.kilivana.backend.common.exception.*;
import com.kilivana.backend.common.repository.EmailVerificationChallengeRepository;
import com.kilivana.backend.mail.*;
import com.kilivana.backend.security.EmailOtpSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class EmailVerificationTransactions {
    private final jakarta.persistence.EntityManager entityManager;
    private final UserRepository users;
    private final EmailVerificationChallengeRepository challenges;
    private final EmailOtpSecurity security;
    private final SendGridMailService mail;
    private final EmailVerificationEmailTemplate template;
    private final com.kilivana.backend.common.repository.EmailOtpRateLimitRepository requests;

    /** Called inside registration/provisioning transaction; no account survives failed initial dispatch. */
    @Transactional
    public EmailOtpChallengeResponse startForNewAccount(User user) {
        UUID id=UUID.randomUUID();
        if(!issue(user,id)) throw new ServiceUnavailableException("Email verification delivery is unavailable. Please try again later.");
        return EmailOtpChallengeResponse.accepted(id);
    }

    @Transactional
    public EmailOtpChallengeResponse request(String email,UUID id) {
        // Request/resend always acknowledge eligible, unknown and already verified addresses identically.
        users.findByEmailIgnoreCase(EmailOtpSecurity.normalize(email)).ifPresent(found -> {
            User user=users.findByIdForUpdate(found.getId()).orElse(null);
            if(user!=null) entityManager.refresh(user);
            if(user!=null && !user.isEmailVerified()
                    && requests.existsByBucketKeyAndLatestChallengeId(security.bucketKey("send-email",EmailOtpSecurity.normalize(email)),id)
                    && EmailOtpSecurity.normalize(user.getEmail()).equals(EmailOtpSecurity.normalize(email)))
                issue(user,id);
        });
        return EmailOtpChallengeResponse.accepted(id);
    }

    @Transactional(readOnly=true)
    public Optional<UUID> activeChallengeId(String email) {
        return users.findByEmailIgnoreCase(EmailOtpSecurity.normalize(email))
                .filter(user -> !user.isEmailVerified())
                .flatMap(user -> challenges.findById(user.getId()))
                .filter(challenge -> challenge.getUsedAt() == null
                        && challenge.getExpiresAt().isAfter(security.now())
                        && challenge.getEmail().equals(EmailOtpSecurity.normalize(email)))
                .map(EmailVerificationChallenge::getChallengeId);
    }

    /** User row lock serializes issuance, attempts and consumption across JVMs. */
    @Transactional(noRollbackFor=EmailOtpInvalidException.class)
    public void verify(EmailOtpVerifyRequest request) {
        var initial=challenges.findByChallengeId(request.getChallengeId()).orElseThrow(EmailOtpInvalidException::new);
        User user=users.findByIdForUpdate(initial.getUserId()).orElseThrow(EmailOtpInvalidException::new);
        entityManager.refresh(user);
        // Re-read after acquiring the account lock: an intervening resend may have rotated the challenge.
        var state=challenges.findById(user.getId()).orElseThrow(EmailOtpInvalidException::new);
        entityManager.refresh(state);
        if(!state.getChallengeId().equals(request.getChallengeId())) throw new EmailOtpInvalidException();
        var now=security.now();
        if(!state.getAttemptsResetAt().isAfter(now)) { state.setFailedAttempts(0);state.setAttemptsResetAt(now.plusSeconds(3600)); }
        boolean bound=state.getEmail().equals(EmailOtpSecurity.normalize(request.getEmail()))
                && state.getEmail().equals(EmailOtpSecurity.normalize(user.getEmail()))
                && state.getRole()==user.getRole() && EmailOtpSecurity.PURPOSE.equals(state.getPurpose());
        boolean correct=security.matches(state.getCodeHash(),security.hash(user,request.getChallengeId(),request.getCode()));
        if(state.getFailedAttempts()>=5 || state.getUsedAt()!=null || !state.getExpiresAt().isAfter(now) || user.isEmailVerified())
            throw new EmailOtpInvalidException();
        if(!bound || !correct) { state.setFailedAttempts(state.getFailedAttempts()+1);challenges.save(state);throw new EmailOtpInvalidException(); }
        state.setUsedAt(now);user.setEmailVerified(true);user.setEmailVerifiedAt(now);
        challenges.save(state);users.save(user);
    }

    private boolean issue(User user,UUID id) {
        var now=security.now();
        var state=challenges.findById(user.getId()).orElseGet(EmailVerificationChallenge::new);
        // Preserve attempt limits across resends and delivery failures.
        if(state.getAttemptsResetAt()!=null && state.getAttemptsResetAt().isAfter(now) && state.getFailedAttempts()>=5) return false;
        String code=security.newCode();String hash=security.hash(user,id,code);
        if(!mail.sendHtml(user.getEmail(),template.subject(user.getRole()),template.render(user.getRole(),code))) return false;
        state.setUserId(user.getId());state.setChallengeId(id);state.setEmail(EmailOtpSecurity.normalize(user.getEmail()));
        state.setRole(user.getRole());state.setPurpose(EmailOtpSecurity.PURPOSE);state.setCodeHash(hash);
        state.setExpiresAt(now.plusSeconds(600));state.setUsedAt(null);
        if(state.getAttemptsResetAt()==null || !state.getAttemptsResetAt().isAfter(now)) {
            state.setFailedAttempts(0);state.setAttemptsResetAt(now.plusSeconds(3600));
        }
        challenges.save(state);return true;
    }
}
