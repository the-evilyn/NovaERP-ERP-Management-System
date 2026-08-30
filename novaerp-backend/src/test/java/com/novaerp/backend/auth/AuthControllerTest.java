package com.novaerp.backend.auth;

import com.novaerp.backend.auth.dto.LoginRequest;
import com.novaerp.backend.auth.dto.RegisterRequest;
import com.novaerp.backend.mail.MailService;
import com.novaerp.backend.security.JwtService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("test@novaerp.local")
                .fullName("Test User")
                .password("encodedPassword")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Login with valid credentials returns 200 and AuthResponse with token")
    void testLogin_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(sampleUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateToken(sampleUser)).thenReturn("sample.jwt.token");

        LoginRequest request = new LoginRequest("test@novaerp.local", "Password123!");
        var response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("sample.jwt.token");
        assertThat(response.getBody().email()).isEqualTo("test@novaerp.local");
        assertThat(response.getBody().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Login with invalid credentials throws UNAUTHORIZED")
    void testLogin_BadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("test@novaerp.local", "WrongPassword");

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("Register with unique email creates user and returns 201 CREATED")
    void testRegister_Success() {
        when(userRepository.existsByEmail("new@novaerp.local")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("new.jwt.token");

        RegisterRequest request = new RegisterRequest("New User", "new@novaerp.local", "Password123!");
        var response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("new.jwt.token");
        assertThat(response.getBody().email()).isEqualTo("new@novaerp.local");

        verify(mailService).sendWelcomeEmail(eq("new@novaerp.local"), eq("New User"));
    }

    @Test
    @DisplayName("Register with existing email throws CONFLICT")
    void testRegister_DuplicateEmail() {
        when(userRepository.existsByEmail("test@novaerp.local")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Test User", "test@novaerp.local", "Password123!");

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("me endpoint returns authenticated user details")
    void testMe() {
        var response = authController.me(sampleUser);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("test@novaerp.local");
        assertThat(response.getBody().fullName()).isEqualTo("Test User");
        assertThat(response.getBody().role()).isEqualTo("USER");
    }
}
