package com.kilivana.backend.security;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.dto.EmailOtpChallengeResponse;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.service.EmailVerificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(ServiceAuthorizationTest.Config.class)
class EmailOtpAuthorizationTest {
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired EmailVerificationService verification;
    @BeforeEach void resetMocks(){reset(users,verification);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void actor(UserRole role,boolean emailVerified) {
        when(users.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).role(role).status(UserStatus.ACTIVE).emailVerified(emailVerified).build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L,null,List.of()));
    }
    @ParameterizedTest @EnumSource(value=UserRole.class,names={"FARMER","SUPPLIER","BUYER","INSPECTOR","DRIVER"})
    void nonAdminsCannotProvisionPrivilegedAccounts(UserRole role) {
        actor(role,true);
        for(var target:List.of(UserRole.ADMIN,UserRole.INSPECTOR))
            assertThrows(AccessDeniedException.class,()->service.createUser(UserRegistrationRequest.builder().role(target).build()));
        verify(users,never()).save(any());verifyNoInteractions(verification);
    }
    @Test void unverifiedAdminCannotUseProvisioningService() {
        actor(UserRole.ADMIN,false);
        assertThrows(AccessDeniedException.class,()->service.createUser(UserRegistrationRequest.builder().role(UserRole.INSPECTOR).build()));
        verify(users,never()).save(any());
    }
    @Test void adminProvisioningCreatesUnverifiedInspectorWithChallenge() {
        actor(UserRole.ADMIN,true);UUID id=UUID.randomUUID();
        when(users.save(any())).thenAnswer(i->{User u=i.getArgument(0);u.setId(9L);return u;});
        when(verification.startForNewAccount(any())).thenReturn(EmailOtpChallengeResponse.accepted(id));
        var response=service.createUser(UserRegistrationRequest.builder().name("Inspector").email("inspector@example.invalid")
            .phone("0700000000").password("test-password").role(UserRole.INSPECTOR).build());
        assertEquals(UserRole.INSPECTOR,response.getRole());assertFalse(response.isEmailVerified());
        assertEquals(id,response.getEmailVerificationChallengeId());
    }
}
