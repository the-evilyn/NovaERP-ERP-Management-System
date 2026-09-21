package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.stock.dto.StockMovementResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementControllerTest {

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StockImportExportService stockImportExportService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private StockMovementController stockMovementController;

    private StockMovement sampleMovement;
    private Article sampleArticle;

    @BeforeEach
    void setUp() {
        sampleArticle = Article.builder().id(1L).reference("ART-01").designation("Test Art").build();
        sampleMovement = StockMovement.builder()
                .id(1L)
                .article(sampleArticle)
                .type(StockMovementType.IN)
                .quantity(new BigDecimal("10.0000"))
                .reference("REF-001")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("export returns CSV attachment from StockImportExportService")
    void testExport() {
        byte[] csv = "id,articleReference\n1,ART-01\n".getBytes(StandardCharsets.UTF_8);
        when(stockImportExportService.exportStockMovements(1L)).thenReturn(csv);

        ResponseEntity<byte[]> response = stockMovementController.export(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(csv);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("stock-movements.csv");
        verify(stockImportExportService).exportStockMovements(1L);
    }

    @Test
    @DisplayName("list returns paginated stock movements")
    void testList() {
        Pageable pageable = PageRequest.of(0, 20);
        when(stockMovementRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleMovement)));

        Page<StockMovementResponse> result = stockMovementController.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("record throws UNAUTHORIZED when authentication is missing or anonymous")
    void testRecord_Unauthenticated() {
        StockMovementRequest request = new StockMovementRequest(1L, StockMovementType.IN, BigDecimal.ONE, "REF", "note");
        assertThatThrownBy(() -> stockMovementController.record(request, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
