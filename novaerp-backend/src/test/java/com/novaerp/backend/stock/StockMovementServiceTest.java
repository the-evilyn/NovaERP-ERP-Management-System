package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private Article sampleArticle;
    private User testUser;

    @BeforeEach
    void setUp() {
        sampleArticle = Article.builder()
                .id(1L)
                .reference("ART-001")
                .designation("Test Article")
                .stockQuantity(new BigDecimal("100.0000"))
                .minStockQuantity(new BigDecimal("10.0000"))
                .build();

        testUser = User.builder()
                .id(1L)
                .email("warehouse@novaerp.local")
                .fullName("Warehouse Operator")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("IN movement increases article stock quantity")
    void testRecordInMovement_IncreasesStock() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("50.0000"), "PO-100", "Inflow"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(StockMovementType.IN);
        assertThat(result.getQuantity()).isEqualByComparingTo("50.0000");
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("150.0000");

        verify(articleRepository).save(sampleArticle);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("OUT movement decreases article stock quantity")
    void testRecordOutMovement_DecreasesStock() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.OUT, new BigDecimal("30.0000"), "SO-200", "Outflow"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(StockMovementType.OUT);
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("70.0000");

        verify(articleRepository).save(sampleArticle);
    }

    @Test
    @DisplayName("OUT movement with insufficient stock throws BAD_REQUEST")
    void testRecordOutMovement_InsufficientStock_ThrowsBadRequest() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.OUT, new BigDecimal("150.0000"), "SO-999", "Too much"
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("cannot go below zero");
                });

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADJUSTMENT with positive quantity increases stock")
    void testRecordAdjustment_PositiveQuantity_IncreasesStock() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("20.0000"), "ADJ-001", "Count surplus"
        );

        stockMovementService.record(request, testUser);

        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("120.0000");
    }

    @Test
    @DisplayName("ADJUSTMENT with negative quantity decreases stock")
    void testRecordAdjustment_NegativeQuantity_DecreasesStock() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("-25.0000"), "ADJ-002", "Defect correction"
        );

        stockMovementService.record(request, testUser);

        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("75.0000");
    }

    @Test
    @DisplayName("ADJUSTMENT driving stock below zero throws BAD_REQUEST")
    void testRecordAdjustment_NegativeQuantityBelowZero_ThrowsBadRequest() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("-150.0000"), "ADJ-003", "Over correction"
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("Zero quantity throws BAD_REQUEST")
    void testRecordMovement_ZeroQuantity_ThrowsBadRequest() {
        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, BigDecimal.ZERO, "PO-0", "Zero"
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("cannot be zero");
                });
    }

    @Test
    @DisplayName("Negative quantity on IN movement throws BAD_REQUEST")
    void testRecordMovement_NegativeIn_ThrowsBadRequest() {
        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("-10"), "PO-ERR", "Negative IN"
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("must be positive");
                });
    }

    @Test
    @DisplayName("Article not found throws NOT_FOUND")
    void testRecordMovement_ArticleNotFound_ThrowsNotFound() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        StockMovementRequest request = new StockMovementRequest(
                999L, StockMovementType.IN, new BigDecimal("10"), "PO-1", "Missing"
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }
}
