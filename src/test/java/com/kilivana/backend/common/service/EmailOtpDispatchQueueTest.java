package com.kilivana.backend.common.service;

import com.kilivana.backend.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailOtpDispatchQueueTest {
    @Test void acknowledgmentDoesNotWaitForAccountLookupOrProviderAndUsesSameOpaqueChallenge() {
        var transactions=mock(EmailVerificationTransactions.class);
        var limiter=mock(EmailOtpRequestLimiter.class);List<Runnable> tasks=new ArrayList<>();
        var queue=new EmailOtpDispatchQueue(transactions,tasks::add);
        when(limiter.acquireSend(anyString())).thenAnswer(i->UUID.randomUUID());
        var service=new EmailVerificationService(limiter,transactions,queue);
        var known=service.request("known@example.invalid");var unknown=service.request("unknown@example.invalid");
        assertEquals(known.message(),unknown.message());assertNotEquals(known.challengeId(),unknown.challengeId());
        assertEquals(2,tasks.size());verifyNoInteractions(transactions);
        tasks.get(0).run();tasks.get(1).run();
        verify(transactions).request("known@example.invalid",known.challengeId());
        verify(transactions).request("unknown@example.invalid",unknown.challengeId());
    }
    @Test void boundedQueueRejectionIsGenericAndDoesNotLookUpAccount() {
        var transactions=mock(EmailVerificationTransactions.class);
        var queue=new EmailOtpDispatchQueue(transactions,task->{throw new TaskRejectedException("test queue full");});
        assertThrows(ServiceUnavailableException.class,()->queue.enqueue("unknown@example.invalid",UUID.randomUUID()));
        verifyNoInteractions(transactions);
    }
    @Test void backgroundFailuresNeverEchoProviderPayloadOrSecret() {
        var transactions=mock(EmailVerificationTransactions.class);
        when(transactions.request(anyString(),any())).thenThrow(new IllegalStateException("test-secret 123456"));
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(EmailOtpDispatchQueue.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            new EmailOtpDispatchQueue(transactions,Runnable::run).enqueue("known@example.invalid",UUID.randomUUID());
            assertTrue(appender.list.stream().allMatch(e->!e.getFormattedMessage().contains("123456")
                && !e.getFormattedMessage().contains("test-secret") && e.getThrowableProxy()==null));
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
