package com.kilivana.backend.repository;

import com.kilivana.backend.admin.dto.*;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.*;
import com.kilivana.backend.common.dto.*;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.*;
import com.kilivana.backend.common.repository.*;
import com.kilivana.backend.common.service.*;
import com.kilivana.backend.mail.*;
import com.kilivana.backend.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Double opt-in, exact disposable URL, synthetic data and a mocked provider only. */
@EnabledIfEnvironmentVariable(named="KILIVANA_RUN_EMAIL_OTP_DB_TESTS",matches="true")
@EnabledIfEnvironmentVariable(named="SPRING_DATASOURCE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/kilivana_audit")
@DataJpaTest(showSql=false,properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","app.seed.enabled=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({EmailVerificationService.class,EmailVerificationTransactions.class,EmailOtpRequestLimiter.class,EmailVerificationEmailTemplate.class,
        AuthService.class,UserService.class,AdminSupplierService.class,AdminDriverService.class,EmailOtpDatabaseTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class EmailOtpDatabaseTest {
    static class TestClock extends Clock {
        final AtomicReference<Instant> value=new AtomicReference<>();
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return this;}
        public Instant instant(){return value.get();}
        void advance(long seconds){value.updateAndGet(v->v.plusSeconds(seconds));}
    }
    @TestConfiguration static class Config {
        @Bean TestClock otpClock(){return new TestClock();}
        @Bean EmailOtpSecurity otpSecurity(TestClock clock){return new EmailOtpSecurity("database-test-only-otp-key-not-a-deployment-secret",clock);}
        @Bean EmailOtpDispatchQueue dispatchQueue(EmailVerificationTransactions transactions) {
            return new EmailOtpDispatchQueue(transactions,Runnable::run);
        }
        @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder();}
        @Bean JwtProperties jwtProperties(){return new JwtProperties();}
    }
    @Autowired EmailVerificationService otp;
    @Autowired EmailVerificationTransactions otpTransactions;
    @Autowired AuthService auth;
    @Autowired UserService provisioning;
    @Autowired AdminSupplierService supplierProvisioning;
    @Autowired AdminDriverService driverProvisioning;
    @Autowired EmailOtpRequestLimiter limiter;
    @Autowired UserRepository users;
    @Autowired EmailVerificationChallengeRepository challenges;
    @Autowired EmailOtpRateLimitRepository limits;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired TestClock clock;
    @MockBean SendGridMailService mail;
    @MockBean JwtService jwt;
    @MockBean UserReferenceCodeGenerator references;
    final ConcurrentMap<String,String> codes=new ConcurrentHashMap<>();
    final AtomicBoolean delivery=new AtomicBoolean(true);
    final AtomicInteger delivered=new AtomicInteger();
    final AtomicInteger sequence=new AtomicInteger();

    @BeforeEach void setup() {
        RequestContextHolder.resetRequestAttributes();
        jdbc.update("delete from email_verification_challenges");jdbc.update("delete from email_otp_rate_limits");
        jdbc.update("delete from supplier_profiles");jdbc.update("delete from driver_profiles");
        users.deleteAll();clock.value.set(Instant.parse("2026-10-21T00:00:00Z"));
        delivery.set(true);codes.clear();delivered.set(0);
        when(references.nextCode(any())).thenAnswer(i->((UserRole)i.getArgument(0)).referenceCode(sequence.incrementAndGet()));
        when(jwt.generateAccessToken(any())).thenReturn("test-access-token");
        when(jwt.generateRefreshToken(any())).thenReturn("test-refresh-token");
        when(mail.sendHtml(anyString(),anyString(),anyString())).thenAnswer(i->{
            if(!delivery.get())return false;
            var m=Pattern.compile(">([0-9]{6})</p>").matcher((String)i.getArgument(2));
            if(!m.find())throw new AssertionError("Verification template must contain a six-digit code");
            codes.put(i.getArgument(0),m.group(1));delivered.incrementAndGet();return true;
        });
    }
    @AfterEach void clear(){RequestContextHolder.resetRequestAttributes();}
    UserRegistrationRequest registration(String email,UserRole role) {
        return UserRegistrationRequest.builder().name("OTP Fixture").email(email).phone("0700"+sequence.incrementAndGet())
            .password("fixture-password").role(role).build();
    }
    UserResponse register(String email){limiter.acquireSend(email);return auth.register(registration(email,UserRole.FARMER));}
    EmailOtpVerifyRequest input(UUID id,String email,String code) {
        var r=new EmailOtpVerifyRequest();r.setChallengeId(id);r.setEmail(email);r.setCode(code);return r;
    }
    void verify(UserResponse created){otp.verify(input(created.getEmailVerificationChallengeId(),created.getEmail(),codes.get(created.getEmail())));}
    String wrong(String good){return good.equals("000000")?"000001":"000000";}
    User saved(Long id){return users.findById(id).orElseThrow();}
    void update(Long id,java.util.function.Consumer<User> change){new TransactionTemplate(transactions).executeWithoutResult(s->{var u=users.findByIdForUpdate(id).orElseThrow();change.accept(u);users.save(u);});}

    @ParameterizedTest @EnumSource(value=UserRole.class,names={"FARMER","SUPPLIER","BUYER","DRIVER"})
    void permittedRegistrationDispatchesThenGatesAuthenticationUntilVerified(UserRole role) {
        var created=auth.register(registration(role.name().toLowerCase()+"@example.invalid",role));
        assertFalse(created.isEmailVerified());assertNotNull(created.getEmailVerificationChallengeId());assertEquals(1,delivered.get());
        AuthLoginRequest login=new AuthLoginRequest();login.setEmail(created.getEmail());login.setPassword("fixture-password");
        assertThrows(ForbiddenException.class,()->auth.login(login));
        var challenge=challenges.findById(created.getId()).orElseThrow();
        assertEquals(64,challenge.getCodeHash().length());assertEquals(role,challenge.getRole());assertEquals("EMAIL_VERIFICATION",challenge.getPurpose());
        verify(created);assertTrue(saved(created.getId()).isEmailVerified());assertNotNull(saved(created.getId()).getEmailVerifiedAt());
        assertEquals("test-access-token",auth.login(login).getAccessToken());assertEquals(role,saved(created.getId()).getRole());
    }
    @Test void provisionedInspectorVerifiesEmailWithoutChangingPendingApproval() {
        var created=provisioning.createUser(registration("inspector@example.invalid",UserRole.INSPECTOR));
        update(created.getId(),u->{u.setStatus(UserStatus.PENDING_VERIFICATION);u.setVerificationStatus(VerificationStatus.PENDING);});
        verify(created);var user=saved(created.getId());assertEquals(UserRole.INSPECTOR,user.getRole());
        assertEquals(UserStatus.PENDING_VERIFICATION,user.getStatus());assertEquals(VerificationStatus.PENDING,user.getVerificationStatus());
    }
    @Test void specializedAdminSupplierProvisioningUsesSameEmailGateAndPreservesPendingStatus() {
        String email="admin-supplier@example.invalid";limiter.acquireSend(email);
        var response=supplierProvisioning.registerSupplier(SupplierRegistrationRequest.builder()
            .companyName("Fixture Supplier").contactPerson("Fixture Contact").username("fixture-supplier")
            .email(email).phone("0700000001").region("Kiambu").category("Fertilizer").password("fixture-password").build());
        assertFalse(response.isEmailVerified());assertNotNull(response.getEmailVerificationChallengeId());
        otp.verify(input(response.getEmailVerificationChallengeId(),email,codes.get(email)));
        assertTrue(saved(response.getUserId()).isEmailVerified());assertEquals(UserRole.SUPPLIER,saved(response.getUserId()).getRole());
        assertEquals(UserStatus.PENDING_VERIFICATION,saved(response.getUserId()).getStatus());
    }
    @Test void specializedAdminDriverProvisioningUsesSameEmailGateAndPreservesKyc() {
        String email="admin-driver@example.invalid";limiter.acquireSend(email);
        var response=driverProvisioning.registerDriver(DriverRegistrationRequest.builder()
            .fullName("Fixture Driver").username("fixture-driver").password("fixture-password").phone("0700000002")
            .email(email).region("Kiambu").idType("National ID").idNumber("12345678").licenceNumber("TEST-DL")
            .licenceExpiry(java.time.LocalDate.of(2030,1,1)).vehicleType(VehicleType.PICKUP).vehicleCapacity("1T").plateNumber("TEST 001").build());
        assertFalse(response.isEmailVerified());assertNotNull(response.getEmailVerificationChallengeId());
        otp.verify(input(response.getEmailVerificationChallengeId(),email,codes.get(email)));
        assertTrue(saved(response.getUserId()).isEmailVerified());assertEquals(UserRole.DRIVER,saved(response.getUserId()).getRole());
        assertEquals(KycStatus.PENDING,driverProvisioning.findDriver(response.getUserId()).orElseThrow().getKycStatus());
    }
    @Test void publicInspectorAndAdminAreRejectedWithoutPersistingOrSending() {
        for(var role:List.of(UserRole.INSPECTOR,UserRole.ADMIN))
            assertThrows(BadRequestException.class,()->auth.register(registration("staff@example.invalid",role)));
        assertEquals(0,users.count());assertEquals(0,challenges.count());assertEquals(0,delivered.get());
    }
    @Test void invalidAttemptsAreCommittedDespiteErrorResponseAndResendDoesNotResetThem() {
        var user=register("attempts@example.invalid");UUID id=user.getEmailVerificationChallengeId();String bad=wrong(codes.get(user.getEmail()));
        for(int i=0;i<2;i++)assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(id,user.getEmail(),bad)));
        assertEquals(2,challenges.findById(user.getId()).orElseThrow().getFailedAttempts());
        clock.advance(61);var next=otp.request(user.getEmail());assertEquals(2,challenges.findById(user.getId()).orElseThrow().getFailedAttempts());
        String badNext=wrong(codes.get(user.getEmail()));
        for(int i=0;i<3;i++)assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(next.challengeId(),user.getEmail(),badNext)));
        assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(next.challengeId(),user.getEmail(),codes.get(user.getEmail()))));
        assertEquals(5,challenges.findById(user.getId()).orElseThrow().getFailedAttempts());assertFalse(saved(user.getId()).isEmailVerified());
    }
    @Test void expiredAndUsedCodesCannotVerify() {
        var user=register("expiry@example.invalid");String code=codes.get(user.getEmail());clock.advance(600);
        assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(user.getEmailVerificationChallengeId(),user.getEmail(),code)));
        var next=otp.request(user.getEmail());otp.verify(input(next.challengeId(),user.getEmail(),codes.get(user.getEmail())));
        assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(next.challengeId(),user.getEmail(),codes.get(user.getEmail()))));
    }
    @Test void anotherAccountEmailCodeAndClientRoleCannotVerifyTarget() {
        var a=register("a@example.invalid");var b=register("b@example.invalid");
        assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(a.getEmailVerificationChallengeId(),b.getEmail(),codes.get(b.getEmail()))));
        assertFalse(saved(a.getId()).isEmailVerified());assertFalse(saved(b.getId()).isEmailVerified());
        verify(a);assertEquals(UserRole.FARMER,saved(a.getId()).getRole());assertFalse(saved(b.getId()).isEmailVerified());
    }
    @Test void correctCodeCannotBypassSuspensionOrApproval() {
        var user=register("suspended@example.invalid");update(user.getId(),u->{u.setStatus(UserStatus.SUSPENDED);u.setVerificationStatus(VerificationStatus.REJECTED);});
        verify(user);var reloaded=saved(user.getId());assertTrue(reloaded.isEmailVerified());
        assertEquals(UserStatus.SUSPENDED,reloaded.getStatus());assertEquals(VerificationStatus.REJECTED,reloaded.getVerificationStatus());
        var login=new AuthLoginRequest();login.setEmail(user.getEmail());login.setPassword("fixture-password");assertThrows(UnauthorizedException.class,()->auth.login(login));
    }
    @Test void deliveryFailureRollsBackInitialRegistrationAndLeavesNoUsableChallenge() {
        delivery.set(false);
        assertThrows(ServiceUnavailableException.class,()->register("failure@example.invalid"));
        assertEquals(0,users.count());assertEquals(0,challenges.count());assertTrue(limits.count()>0);
    }
    @Test void failedResendRetainsWorkingCodeWhileReturningGenericAcknowledgment() {
        var user=register("resend-failure@example.invalid");String code=codes.get(user.getEmail());delivery.set(false);clock.advance(61);
        var response=otp.request(user.getEmail());var unknown=otp.request("unknown@example.invalid");
        assertEquals(response.message(),unknown.message());assertNotNull(response.challengeId());assertNotNull(unknown.challengeId());
        assertEquals(user.getEmailVerificationChallengeId(),challenges.findById(user.getId()).orElseThrow().getChallengeId());
        otp.verify(input(user.getEmailVerificationChallengeId(),user.getEmail(),code));assertTrue(saved(user.getId()).isEmailVerified());
    }
    @Test void successfulResendInvalidatesOldChallengeAndEmailChangeRequiresFreshProof() {
        var user=register("rotation@example.invalid");String oldCode=codes.get(user.getEmail());clock.advance(61);
        var next=otp.request(user.getEmail());
        assertThrows(EmailOtpInvalidException.class,()->otp.verify(input(user.getEmailVerificationChallengeId(),user.getEmail(),oldCode)));
        otp.verify(input(next.challengeId(),user.getEmail(),codes.get(user.getEmail())));
        update(user.getId(),u->u.setEmail("new-address@example.invalid"));assertFalse(saved(user.getId()).isEmailVerified());
        assertNull(saved(user.getId()).getEmailVerifiedAt());
        var changed=otp.request("new-address@example.invalid");
        otp.verify(input(changed.challengeId(),"new-address@example.invalid",codes.get("new-address@example.invalid")));
        assertTrue(saved(user.getId()).isEmailVerified());
    }
    @Test void knownAndUnknownAddressesHaveIdenticalCooldownAndHourlyLimits() {
        var user=register("limits@example.invalid");
        var unknownFirst=otp.request("unknown-limits@example.invalid");
        for(String email:List.of(user.getEmail(),"unknown-limits@example.invalid"))
            assertNotNull(otp.request(email).challengeId());
        assertEquals(user.getEmailVerificationChallengeId(),otp.request(user.getEmail()).challengeId());
        assertEquals(unknownFirst.message(),otp.request("unknown-limits@example.invalid").message());
        assertEquals(1,delivered.get());
        for(int i=1;i<5;i++){clock.advance(61);otp.request(user.getEmail());otp.request("unknown-limits@example.invalid");}
        clock.advance(61);
        for(String email:List.of(user.getEmail(),"unknown-limits@example.invalid"))
            assertThrows(EmailOtpRateLimitException.class,()->otp.request(email));
        assertEquals(5,delivered.get());
    }
    @Test void delayedOldQueuedRequestCannotInvalidateANewerResend() {
        var user=register("queue-order@example.invalid");clock.advance(61);
        var oldTicket=limiter.acquireSend(user.getEmail());clock.advance(61);
        var latest=otp.request(user.getEmail());String code=codes.get(user.getEmail());
        otpTransactions.request(user.getEmail(),oldTicket);
        assertEquals(latest.challengeId(),challenges.findById(user.getId()).orElseThrow().getChallengeId());
        assertEquals(2,delivered.get());otp.verify(input(latest.challengeId(),user.getEmail(),code));
        assertTrue(saved(user.getId()).isEmailVerified());
    }
    @Test void cooldownSurvivesHourlyCounterRollover() {
        var user=register("rollover@example.invalid");clock.advance(3590);otp.request(user.getEmail());
        clock.advance(11);assertThrows(EmailOtpRateLimitException.class,()->otp.request(user.getEmail()));
        clock.advance(49);otp.request(user.getEmail());assertEquals(3,delivered.get());
    }
    @Test void sharedIpBudgetCannotBeBypassedByForwardedHeadersOrNewEmails() {
        var request=new MockHttpServletRequest();request.setRemoteAddr("192.0.2.1");request.addHeader("X-Forwarded-For","198.51.100.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        for(int i=0;i<60;i++)otp.request("unknown-"+i+"@example.invalid");
        request.addHeader("X-Forwarded-For","198.51.100.99");
        assertThrows(EmailOtpRateLimitException.class,()->otp.request("another@example.invalid"));assertEquals(0,delivered.get());
    }
    @Test void concurrentCorrectVerificationsHaveExactlyOneWinner() throws Exception {
        var user=register("race-success@example.invalid");String code=codes.get(user.getEmail());
        var result=race(12,()->{try {otp.verify(input(user.getEmailVerificationChallengeId(),user.getEmail(),code));return true;}catch(EmailOtpInvalidException ex){return false;}});
        assertEquals(1,result.stream().filter(Boolean::booleanValue).count());assertTrue(saved(user.getId()).isEmailVerified());
        assertNotNull(challenges.findById(user.getId()).orElseThrow().getUsedAt());
    }
    @Test void concurrentIncorrectRequestsCannotExceedAttemptBudget() throws Exception {
        var user=register("race-attempts@example.invalid");String bad=wrong(codes.get(user.getEmail()));
        var result=race(12,()->{try{otp.verify(input(user.getEmailVerificationChallengeId(),user.getEmail(),bad));return true;}catch(EmailOtpInvalidException ex){return false;}});
        assertEquals(0,result.stream().filter(Boolean::booleanValue).count());
        assertEquals(5,challenges.findById(user.getId()).orElseThrow().getFailedAttempts());assertFalse(saved(user.getId()).isEmailVerified());
    }
    @Test void concurrentResendsCannotBypassCooldownOrSendMultipleCodes() throws Exception {
        var user=register("race-resend@example.invalid");clock.advance(61);
        var result=race(6,()->{try{otp.request(user.getEmail());return true;}catch(EmailOtpRateLimitException ex){return false;}});
        assertEquals(1,result.stream().filter(Boolean::booleanValue).count());assertEquals(2,delivered.get());
    }
    List<Boolean> race(int count,Callable<Boolean> action) throws Exception {
        var pool=Executors.newFixedThreadPool(count);var start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(int i=0;i<count;i++)futures.add(pool.submit(()->{start.await();return action.call();}));
            start.countDown();List<Boolean> results=new ArrayList<>();
            for(var future:futures)results.add(future.get(20,TimeUnit.SECONDS));return results;
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void migrationGrandfathersExistingApprovedUsersWithoutChangingRoles() throws Exception {
        String schema="otp_migration_"+UUID.randomUUID().toString().replace("-","");
        String migration=Files.readString(Path.of("db/migrations/V20__email_otp_verification.sql"));
        jdbc.execute((ConnectionCallback<Void>)connection->{
            try(var statement=connection.createStatement()) {
                statement.execute("create schema "+schema);
                statement.execute("set search_path to "+schema);
                statement.execute("create table users (id bigint primary key, role varchar(255), verification_status varchar(255))");
                statement.execute("insert into users values (1,'FARMER','VERIFIED'),(2,'INSPECTOR','PENDING')");
                statement.execute(migration);
                statement.execute("insert into users (id,role,verification_status) values (3,'BUYER','NOT_REQUIRED')");
                try(var fresh=statement.executeQuery("select email_verified from users where id=3")){assertTrue(fresh.next());assertFalse(fresh.getBoolean(1));}
                try(var result=statement.executeQuery("select count(*) from users where email_verified=true and email_verified_at is null")){assertTrue(result.next());assertEquals(2,result.getInt(1));}
                try(var result=statement.executeQuery("select role,verification_status from users where id=1")){assertTrue(result.next());assertEquals("FARMER",result.getString(1));assertEquals("VERIFIED",result.getString(2));}
                try(var result=statement.executeQuery("select role,verification_status from users where id=2")){assertTrue(result.next());assertEquals("INSPECTOR",result.getString(1));assertEquals("PENDING",result.getString(2));}
            } finally {try(var statement=connection.createStatement()){statement.execute("set search_path to public");}}
            return null;
        });
    }
}
