package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.UserRegistrationRequest;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceUsernameRegionTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserReferenceCodeGenerator referenceCodeGenerator;
    @Mock private DriverProfileRepository driverProfileRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, passwordEncoder, referenceCodeGenerator, driverProfileRepository);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(referenceCodeGenerator.nextCode(any())).thenReturn("F-001");
        org.mockito.Mockito.lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
    }

    private UserRegistrationRequest request(String region) {
        return UserRegistrationRequest.builder()
                .name("Jane Wanjiku")
                .email("jane.wanjiku@example.com")
                .phone("+254712345678")
                .password("Kilivana#2026")
                .role(UserRole.FARMER)
                .username("jane.farmer")
                .region(region)
                .build();
    }

    @Test
    @DisplayName("creates a user with a username and a Kenyan county")
    void persistsUsernameAndCounty() {
        UserResponse response = userService.createUser(request("Kiambu"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("jane.farmer", saved.getUsername());
        assertEquals("Kiambu", saved.getRegion());
        assertEquals("jane.wanjiku@example.com", response.getEmail());
    }

    @Test
    @DisplayName("a county sent in any spelling is stored in the panel's spelling")
    void normalisesCountySpelling() {
        userService.createUser(request("kiambu"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertEquals("Kiambu", captor.getValue().getRegion());
    }

    @Test
    @DisplayName("refuses a region that is not one of the 47 Kenyan counties")
    void refusesNonKenyanRegion() {
        assertThrows(com.kilivana.backend.common.exception.BadRequestException.class,
                () -> userService.createUser(request("Ashanti")));
    }

    @Test
    @DisplayName("updates the county when the region is present")
    void updateChangesCounty() {
        User existing = User.builder()
                .id(1L)
                .name("Jane Wanjiku")
                .email("jane.wanjiku@example.com")
                .phone("+254712345678")
                .region("Nairobi")
                .passwordHash("hashed")
                .role(UserRole.FARMER)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.updateUser(1L, request("Murang'a"));

        assertEquals("Murang'a", existing.getRegion());
    }
}
