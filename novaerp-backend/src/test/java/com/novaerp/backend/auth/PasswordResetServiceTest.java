package com.novaerp.backend.auth;

import com.novaerp.backend.mail.MailService;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "resetPasswordFrontendUrl", "http://localhost:3000/reset-password");

        sampleUser = User.builder()
                .id(10L)
                .email("user@novaerp.local")
                .fullName("Sara Amrani")
                .password("oldHashedPassword")
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("requestReset() creates token and sends email for existing user")
    void testRequestReset_UserExists() {
        when(userRepository.findByEmail("user@novaerp.local")).thenReturn(Optional.of(sampleUser));

        passwordResetService.requestReset("user@novaerp.local");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetEmail(eq("user@novaerp.local"), eq("Sara Amrani"), anyString());
    }

    @Test
    @DisplayName("requestReset() does nothing when email unknown (no leak)")
    void testRequestReset_UnknownEmail() {
        when(userRepository.findByEmail("unknown@novaerp.local")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@novaerp.local");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resetPassword() successfully updates password for valid token")
    void testResetPassword_Success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash("someHash")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .createdAt(Instant.now())
                .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("encodedNewPass");

        passwordResetService.resetPassword("rawTokenXYZ", "NewPass123!");

        assertThat(sampleUser.getPassword()).isEqualTo("encodedNewPass");
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(sampleUser);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    @DisplayName("resetPassword() throws BAD_REQUEST when token not found")
    void testResetPassword_TokenNotFound() {
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalidToken", "NewPass123!"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("resetPassword() throws BAD_REQUEST when token already used")
    void testResetPassword_AlreadyUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash("someHash")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .usedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .createdAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("usedToken", "NewPass123!"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("resetPassword() throws BAD_REQUEST when token is expired")
    void testResetPassword_Expired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash("someHash")
                .expiresAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .createdAt(Instant.now().minus(35, ChronoUnit.MINUTES))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expiredToken", "NewPass123!"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
