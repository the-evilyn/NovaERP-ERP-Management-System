package com.novaerp.backend.security;

import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Valid Base64 256-bit key for testing
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes()
    );
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
        testUser = User.builder()
                .id(1L)
                .email("user@novaerp.local")
                .fullName("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("generateToken creates a valid JWT from which username and role can be extracted")
    void testGenerateTokenAndExtractUsername() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("user@novaerp.local");
        String extractedRole = jwtService.extractRole(token);
        assertThat(extractedRole).isEqualTo("USER");
    }

    @Test
    @DisplayName("generateToken preserves ADMIN role in token")
    void testGenerateTokenWithAdminRole() {
        User adminUser = User.builder()
                .id(2L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();

        String token = jwtService.generateToken(adminUser);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("isTokenValid returns true for matching user details")
    void testIsTokenValid_Success() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for mismatched user details")
    void testIsTokenValid_MismatchedUser() {
        String token = jwtService.generateToken(testUser);

        User otherUser = User.builder()
                .id(2L)
                .email("other@novaerp.local")
                .fullName("Other User")
                .role(Role.USER)
                .build();

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Expired token throws ExpiredJwtException on extraction")
    void testExpiredToken_ThrowsException() {
        JwtService shortLivedService = new JwtService(TEST_SECRET, -1000); // already expired
        String expiredToken = shortLivedService.generateToken(testUser);

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Malformed token throws MalformedJwtException")
    void testMalformedToken_ThrowsException() {
        assertThatThrownBy(() -> jwtService.extractUsername("invalid.token.payload"))
                .isInstanceOf(Exception.class);
    }
}
