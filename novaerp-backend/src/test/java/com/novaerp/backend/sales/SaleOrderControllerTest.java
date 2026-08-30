package com.novaerp.backend.sales;

import com.novaerp.backend.sales.dto.SaleOrderItemRequest;
import com.novaerp.backend.sales.dto.SaleOrderRequest;
import com.novaerp.backend.sales.dto.SaleOrderResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleOrderControllerTest {

    @Mock
    private SaleOrderService saleOrderService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SaleOrderController saleOrderController;

    private User sampleUser;
    private Authentication sampleAuth;
    private SaleOrderResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        sampleAuth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        sampleResponse = new SaleOrderResponse(
                100L,
                "SO-2026-0001",
                1L,
                "Client Test",
                "Casablanca",
                SaleOrderStatus.DRAFT,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                null,
                null,
                List.of()
        );
    }

    @Test
    @DisplayName("list() delegates to service and returns page")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SaleOrderResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(saleOrderService.list(null, null, pageable)).thenReturn(page);

        Page<SaleOrderResponse> result = saleOrderController.list(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).orderNumber()).isEqualTo("SO-2026-0001");
        verify(saleOrderService).list(null, null, pageable);
    }

    @Test
    @DisplayName("getById() returns order details")
    void testGetById() {
        when(saleOrderService.getById(100L)).thenReturn(sampleResponse);

        SaleOrderResponse result = saleOrderController.getById(100L);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.orderNumber()).isEqualTo("SO-2026-0001");
    }

    @Test
    @DisplayName("create() returns 201 with created order")
    void testCreate() {
        SaleOrderRequest req = new SaleOrderRequest(1L, List.of(
                new SaleOrderItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)
        ), null, "Notes");

        when(saleOrderService.create(eq(req), any())).thenReturn(sampleResponse);

        ResponseEntity<SaleOrderResponse> result = saleOrderController.create(req, sampleAuth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().orderNumber()).isEqualTo("SO-2026-0001");
    }

    @Test
    @DisplayName("confirm() triggers confirmation and returns updated response")
    void testConfirm() {
        SaleOrderResponse confirmed = new SaleOrderResponse(
                100L,
                "SO-2026-0001",
                1L,
                "Client Test",
                "Casablanca",
                SaleOrderStatus.CONFIRMED,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );

        when(saleOrderService.confirm(eq(100L), any())).thenReturn(confirmed);

        SaleOrderResponse result = saleOrderController.confirm(100L, sampleAuth);

        assertThat(result.status()).isEqualTo(SaleOrderStatus.CONFIRMED);
        verify(saleOrderService).confirm(eq(100L), any());
    }

    @Test
    @DisplayName("cancel() triggers cancellation")
    void testCancel() {
        SaleOrderResponse cancelled = new SaleOrderResponse(
                100L,
                "SO-2026-0001",
                1L,
                "Client Test",
                "Casablanca",
                SaleOrderStatus.CANCELLED,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                null,
                null,
                List.of()
        );

        when(saleOrderService.cancel(eq(100L), any())).thenReturn(cancelled);

        SaleOrderResponse result = saleOrderController.cancel(100L, sampleAuth);

        assertThat(result.status()).isEqualTo(SaleOrderStatus.CANCELLED);
        verify(saleOrderService).cancel(eq(100L), any());
    }
}
