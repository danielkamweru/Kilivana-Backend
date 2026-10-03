package com.kilivana.backend.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The admin panel collects a username and a region on every account form. They were accepted in
 * the request and then dropped, so the created account had neither to display.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceUsernameRegionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.kilivana.backend.common.service.UserReferenceCodeGenerator referenceCodeGenerator;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, referenceCodeGenerator, driverProfileRepository);
    }

    @Test
    void createUserKeepsUsernameAndRegion() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);

        UserResponse response = service.createUser(UserRegistrationRequest.builder()
                .name("Amara Osei")
                .email("amara.osei@mail.com")
                .phone("+233241112233")
                .password("secret123")
                .role(UserRole.FARMER)
                .username("amara.osei")
                .region("Ashanti")
                .build());

        assertEquals("amara.osei", response.getUsername());
        assertEquals("Ashanti", response.getRegion());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(saved.capture());
        assertEquals("amara.osei", saved.getValue().getUsername());
        assertEquals("Ashanti", saved.getValue().getRegion());
    }

    @Test
    void updateUserRejectsAUsernameAnotherAccountHolds() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(User.builder()
                .id(5L).email("a@b.com").username("mine").build()));
        when(userRepository.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.updateUser(5L, UserRegistrationRequest.builder()
                .name("A").email("a@b.com").phone("0700000000").role(UserRole.FARMER)
                .username("taken").region("Ashanti").build()));
    }

    @Test
    void updateUserKeepsTheUsernameWhenItIsUnchanged() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(User.builder()
                .id(5L).email("a@b.com").username("mine").build()));
        // Another row owns "mine"; the owner may still keep it.
        when(userRepository.existsByUsernameIgnoreCase("mine")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.updateUser(5L, UserRegistrationRequest.builder()
                .name("A").email("a@b.com").phone("0700000000").role(UserRole.FARMER)
                .username("mine").region("Volta").build());

        assertEquals("mine", response.getUsername());
        assertEquals("Volta", response.getRegion());
    }
}