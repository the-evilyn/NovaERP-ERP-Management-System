package com.novaerp.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("handleResponseStatus returns corresponding status and message")
    void testHandleResponseStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResponseStatus(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(404);
        assertThat(response.getBody().get("message")).isEqualTo("Article not found");
        assertThat(response.getBody().get("path")).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation with foreign key violation returns 409 Conflict")
    void testHandleDataIntegrityViolation_ForeignKey() {
        Throwable cause = new RuntimeException("violates foreign key constraint fk_article_category");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("FK error", cause);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat(response.getBody().get("message").toString()).contains("référencée par d'autres enregistrements");
        assertThat(response.getBody().get("path")).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("handleDataIntegrityViolation with unique constraint violation returns 409 Conflict")
    void testHandleDataIntegrityViolation_UniqueConstraint() {
        Throwable cause = new RuntimeException("violates unique constraint uk_user_email");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique error", cause);

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(409);
        assertThat(response.getBody().get("message").toString()).contains("contrainte d'unicité");
    }
}
