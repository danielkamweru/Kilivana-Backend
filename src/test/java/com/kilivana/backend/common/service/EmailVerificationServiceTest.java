package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.dto.*;
import com.kilivana.backend.common.entity.EmailVerificationChallenge;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.*;
import com.kilivana.backend.common.repository.EmailVerificationChallengeRepository;
import com.kilivana.backend.mail.*;
import com.kilivana.backend.security.EmailOtpSecurity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    final UserRepository users=mock(UserRepository.class);
    final EmailVerificationChallengeRepository states=mock(EmailVerificationChallengeRepository.class);
    final com.kilivana.backend.common.repository.EmailOtpRateLimitRepository requests=mock(com.kilivana.backend.common.repository.EmailOtpRateLimitRepository.class);
    final SendGridMailService mail=mock(SendGridMailService.class);
    final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-10-21T00:00:00Z"));
    final Clock clock=new Clock() {
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;}
        public Instant instant(){return now.get();}
    };
    final EmailOtpSecurity security=new EmailOtpSecurity("unit-test-only-otp-key-with-at-least-32-bytes",clock);
    final AtomicReference<EmailVerificationChallenge> state=new AtomicReference<>();
    final AtomicReference<String> html=new AtomicReference<>();
    final AtomicReference<String> subject=new AtomicReference<>();
    EmailVerificationTransactions service;
    User user;
    @BeforeEach void setup() {
        user=User.builder().id(7L).email("farmer@example.invalid").role(UserRole.FARMER)
            .emailVerified(false).status(UserStatus.PENDING_VERIFICATION).verificationStatus(VerificationStatus.PENDING).build();
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase(anyString())).thenAnswer(i->i.getArgument(0).equals(user.getEmail())?Optional.of(user):Optional.empty());
        when(states.findById(7L)).thenAnswer(i->Optional.ofNullable(state.get()));
        when(states.findByChallengeId(any())).thenAnswer(i->Optional.ofNullable(state.get()).filter(s->s.getChallengeId().equals(i.getArgument(0))));
        when(states.save(any())).thenAnswer(i->{var s=(EmailVerificationChallenge)i.getArgument(0);state.set(s);return s;});
        when(mail.sendHtml(anyString(),anyString(),anyString())).thenAnswer(i->{subject.set(i.getArgument(1));html.set(i.getArgument(2));return true;});
        when(requests.existsByBucketKeyAndLatestChallengeId(anyString(),any())).thenReturn(true);
        service=new EmailVerificationTransactions(mock(EntityManager.class),users,states,security,mail,new EmailVerificationEmailTemplate(),requests);
    }
    String code() {
        var m=Pattern.compile(">([0-9]{6})</p>").matcher(html.get());assertTrue(m.find());return m.group(1);
    }
    EmailOtpVerifyRequest request(UUID id,String email,String code) {
        var r=new EmailOtpVerifyRequest();r.setChallengeId(id);r.setEmail(email);r.setCode(code);return r;
    }
    @ParameterizedTest @EnumSource(UserRole.class)
    void roleAwareDispatchAndVerificationNeverChangesRoleOrApproval(UserRole role) {
        user.setRole(role);
        var response=service.startForNewAccount(user);
        String raw=code();assertTrue(raw.matches("[0-9]{6}"));
        String label=new EmailVerificationEmailTemplate().roleLabel(role);
        assertEquals("Kilivana \u2014 "+label+" Email Verification",subject.get());
        assertTrue(html.get().contains(label+" account"));assertTrue(html.get().contains("10 minutes"));
        assertTrue(html.get().contains("Do not share"));assertTrue(html.get().contains("did not request"));
        assertEquals(64,state.get().getCodeHash().length());assertNotEquals(raw,state.get().getCodeHash());
        assertEquals(now.get().plusSeconds(600),state.get().getExpiresAt());
        service.verify(request(response.challengeId(),user.getEmail(),raw));
        assertTrue(user.isEmailVerified());assertEquals(now.get(),user.getEmailVerifiedAt());assertNotNull(state.get().getUsedAt());
        assertEquals(role,user.getRole());assertEquals(UserStatus.PENDING_VERIFICATION,user.getStatus());
        assertEquals(VerificationStatus.PENDING,user.getVerificationStatus());
    }
    @Test void wrongCodeAndEmailConsumeAttemptsButDoNotVerify() {
        var c=service.startForNewAccount(user);String good=code();String wrong=good.equals("000000")?"000001":"000000";
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),wrong)));
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),"other@example.invalid",good)));
        assertEquals(2,state.get().getFailedAttempts());assertFalse(user.isEmailVerified());
    }
    @Test void expiresAtTenMinutesAndCannotBeUsedTwice() {
        var c=service.startForNewAccount(user);String raw=code();now.set(now.get().plusSeconds(600));
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),raw)));
        now.set(now.get().minusSeconds(1));service.verify(request(c.challengeId(),user.getEmail(),raw));
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),raw)));
    }
    @Test void fiveFailuresLockEvenCorrectCodeAndResendCannotResetBudget() {
        var c=service.startForNewAccount(user);String good=code();String wrong=good.equals("000000")?"000001":"000000";
        for(int i=0;i<7;i++) assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),wrong)));
        assertEquals(5,state.get().getFailedAttempts());
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),good)));
        clearInvocations(mail);service.request(user.getEmail(),UUID.randomUUID());verifyNoInteractions(mail);
        assertEquals(5,state.get().getFailedAttempts());assertFalse(user.isEmailVerified());
    }
    @Test void resendRotatesChallengeWithoutResettingAttempts() {
        var c=service.startForNewAccount(user);String good=code();String wrong=good.equals("000000")?"000001":"000000";
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),wrong)));
        now.set(now.get().plusSeconds(61));var replacement=service.request(user.getEmail(),UUID.randomUUID());
        assertNotEquals(c.challengeId(),replacement.challengeId());assertEquals(1,state.get().getFailedAttempts());
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),good)));
        service.verify(request(replacement.challengeId(),user.getEmail(),code()));assertTrue(user.isEmailVerified());
    }
    @Test void failuresRemainGenericAndFailedResendPreservesPreviousChallenge() {
        var c=service.startForNewAccount(user);String raw=code();
        when(mail.sendHtml(anyString(),anyString(),anyString())).thenReturn(false);
        now.set(now.get().plusSeconds(61));var failure=service.request(user.getEmail(),UUID.randomUUID());
        var unknown=service.request("unknown@example.invalid",UUID.randomUUID());
        assertEquals(failure.message(),unknown.message());assertEquals(failure.expiresInSeconds(),unknown.expiresInSeconds());
        assertEquals(c.challengeId(),state.get().getChallengeId());
        service.verify(request(c.challengeId(),user.getEmail(),raw));assertTrue(user.isEmailVerified());
    }
    @Test void initialDeliveryFailureDoesNotLeaveAnActiveCode() {
        when(mail.sendHtml(anyString(),anyString(),anyString())).thenReturn(false);
        assertThrows(ServiceUnavailableException.class,()->service.startForNewAccount(user));
        verify(states,never()).save(any());assertFalse(user.isEmailVerified());
    }
    @Test void unknownAndVerifiedAddressesHaveSameResponseAndNoDispatch() {
        var unknown=service.request("unknown@example.invalid",UUID.randomUUID());user.setEmailVerified(true);
        var verified=service.request(user.getEmail(),UUID.randomUUID());
        assertEquals(unknown.message(),verified.message());assertNotNull(unknown.challengeId());assertNotNull(verified.challengeId());
        verifyNoInteractions(mail);verify(states,never()).save(any());
    }
    @Test void codeHashIsBoundToAccountEmailRolePurposeAndChallenge() {
        UUID id=UUID.randomUUID();String digest=security.hash(user,id,"123456");
        assertNotEquals(digest,security.hash(user,UUID.randomUUID(),"123456"));
        user.setRole(UserRole.INSPECTOR);assertNotEquals(digest,security.hash(user,id,"123456"));
        user.setRole(UserRole.FARMER);user.setId(8L);assertNotEquals(digest,security.hash(user,id,"123456"));
        user.setId(7L);user.setEmail("changed@example.invalid");assertNotEquals(digest,security.hash(user,id,"123456"));
        assertFalse(security.matches(digest,security.bucketKey("EMAIL_VERIFICATION","123456")));
    }
    @Test void emailOrRoleChangeInvalidatesOutstandingCode() {
        var c=service.startForNewAccount(user);String raw=code();user.setRole(UserRole.ADMIN);
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),user.getEmail(),raw)));
        user.setRole(UserRole.FARMER);user.setEmail("changed@example.invalid");
        assertThrows(EmailOtpInvalidException.class,()->service.verify(request(c.challengeId(),"farmer@example.invalid",raw)));
        assertFalse(user.isEmailVerified());
    }
    @Test void supersededQueuedRequestCannotReplaceLatestChallenge() {
        var first=service.startForNewAccount(user);clearInvocations(mail);
        when(requests.existsByBucketKeyAndLatestChallengeId(anyString(),any())).thenReturn(false);
        service.request(user.getEmail(),UUID.randomUUID());
        verifyNoInteractions(mail);assertEquals(first.challengeId(),state.get().getChallengeId());
    }
    @Test void immediateRetryReusesActiveChallengeWithoutSendingAgain() {
        var limiter=mock(EmailOtpRequestLimiter.class);
        var transactions=mock(EmailVerificationTransactions.class);
        var queue=mock(EmailOtpDispatchQueue.class);
        var verification=new EmailVerificationService(limiter,transactions,queue);
        var challengeId=UUID.randomUUID();
        when(limiter.acquireSend(user.getEmail())).thenThrow(new EmailOtpCooldownException(60));
        when(transactions.activeChallengeId(user.getEmail())).thenReturn(Optional.of(challengeId));

        var response=verification.request(user.getEmail());

        assertEquals(challengeId,response.challengeId());
        verifyNoInteractions(queue);
    }
    @Test void cooldownRetryUsesChallengeFromTheInitialRegistrationEmail() {
        var registration=service.startForNewAccount(user);

        assertEquals(registration.challengeId(),service.activeChallengeId(user.getEmail()).orElseThrow());
    }
    @Test void hourlyRateLimitStillRejectsRequests() {
        var limiter=mock(EmailOtpRequestLimiter.class);
        var transactions=mock(EmailVerificationTransactions.class);
        var queue=mock(EmailOtpDispatchQueue.class);
        var verification=new EmailVerificationService(limiter,transactions,queue);
        when(limiter.acquireSend(user.getEmail())).thenThrow(new EmailOtpRateLimitException(3600));

        assertThrows(EmailOtpRateLimitException.class,()->verification.request(user.getEmail()));
        verifyNoInteractions(transactions,queue);
    }
    @Test void missingOrShortKeyFailsClosed() {
        assertThrows(IllegalStateException.class,()->new EmailOtpSecurity(""));
        assertThrows(IllegalStateException.class,()->new EmailOtpSecurity("short"));
    }
}
