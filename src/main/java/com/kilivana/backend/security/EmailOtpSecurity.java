package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Component
public class EmailOtpSecurity {
    public static final String PURPOSE="EMAIL_VERIFICATION";
    private static final SecureRandom RANDOM=new SecureRandom();
    private final byte[] key;
    private final Clock clock;

    @Autowired
    public EmailOtpSecurity(@Value("${app.email-verification.hmac-secret:}") String secret) {
        this(secret,Clock.systemUTC());
    }
    public EmailOtpSecurity(String secret,Clock clock) {
        if(secret==null || secret.getBytes(StandardCharsets.UTF_8).length<32)
            throw new IllegalStateException("EMAIL_OTP_HMAC_SECRET must contain at least 32 bytes and be shared by all application instances");
        this.key=secret.getBytes(StandardCharsets.UTF_8);this.clock=clock;
    }
    public Instant now() { return clock.instant(); }
    public String newCode() { return String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000)); }
    public static String normalize(String email) { return email==null ? "" : email.trim().toLowerCase(Locale.ROOT); }
    public String bucketKey(String scope,String identity) { return digest("RATE\0"+scope+"\0"+identity); }
    public String hash(User user,UUID challengeId,String code) {
        return digest(PURPOSE+"\0"+user.getId()+"\0"+normalize(user.getEmail())+"\0"+user.getRole().name()+"\0"+challengeId+"\0"+code);
    }
    public boolean matches(String expected,String actual) {
        return expected!=null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII));
    }
    private String digest(String input) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch(GeneralSecurityException ex) { throw new IllegalStateException("Email verification cryptography unavailable"); }
    }
}
