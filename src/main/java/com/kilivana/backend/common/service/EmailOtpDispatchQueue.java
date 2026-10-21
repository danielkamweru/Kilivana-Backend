package com.kilivana.backend.common.service;

import com.kilivana.backend.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

/** Queue every accepted resend before looking up its account; no OTP is stored in queued jobs. */
@Service
public class EmailOtpDispatchQueue implements DisposableBean {
    private static final Logger log=LoggerFactory.getLogger(EmailOtpDispatchQueue.class);
    private final EmailVerificationTransactions transactions;
    private final TaskExecutor executor;
    private final ThreadPoolTaskExecutor ownedExecutor;

    @Autowired
    public EmailOtpDispatchQueue(EmailVerificationTransactions transactions) {
        this.transactions=transactions;
        var pool=new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);pool.setMaxPoolSize(2);pool.setQueueCapacity(100);
        pool.setThreadNamePrefix("email-verification-");pool.setDaemon(true);
        pool.setWaitForTasksToCompleteOnShutdown(false);pool.initialize();
        this.executor=pool;this.ownedExecutor=pool;
    }
    /** Deterministic executor injection for isolated tests; production owns a bounded pool. */
    public EmailOtpDispatchQueue(EmailVerificationTransactions transactions,TaskExecutor executor) {
        this.transactions=transactions;this.executor=executor;this.ownedExecutor=null;
    }
    public void enqueue(String email,UUID challengeId) {
        try {
            executor.execute(()->{
                try { transactions.request(email,challengeId); }
                catch(RuntimeException ex) { log.warn("Email verification dispatch failed"); }
            });
        } catch(RejectedExecutionException ex) {
            throw new ServiceUnavailableException("Email verification delivery is unavailable. Please try again later.");
        }
    }
    @Override public void destroy() { if(ownedExecutor!=null)ownedExecutor.shutdown(); }
}
