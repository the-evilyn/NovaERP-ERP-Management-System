package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockTransferItemRequest;
import com.novaerp.backend.stock.dto.StockTransferRequest;
import com.novaerp.backend.stock.dto.StockTransferResponse;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private StockTransferItemRepository stockTransferItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockTransferService stockTransferService;

    private Warehouse srcWarehouse;
    private Warehouse dstWarehouse;
    private WarehouseLocation srcLocation;
    private WarehouseLocation dstLocation;
    private Article sampleArticle;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        srcWarehouse = Warehouse.builder()
                .id(1L)
                .code("WH-MAIN")
                .name("Entrepôt Principal")
                .active(true)
                .build();

        dstWarehouse = Warehouse.builder()
                .id(2L)
                .code("WH-TNG")
                .name("Entrepôt Tanger")
                .active(true)
                .build();

        srcLocation = WarehouseLocation.builder()
                .id(10L)
                .warehouse(srcWarehouse)
                .code("LOC-SRC")
                .name("Emplacement Source")
                .active(true)
                .build();

        dstLocation = WarehouseLocation.builder()
                .id(20L)
                .warehouse(dstWarehouse)
                .code("LOC-DST")
                .name("Emplacement Destination")
                .active(true)
                .build();

        sampleArticle = Article.builder()
                .id(100L)
                .reference("ART-TEST-001")
                .designation("Article Test")
                .stockQuantity(new BigDecimal("50.0000"))
                .build();

        sampleUser = User.builder()
                .id(99L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Create draft transfer successfully")
    void testCreateDraftTransfer_Success() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(dstWarehouse));
        when(warehouseLocationRepository.findById(10L)).thenReturn(Optional.of(srcLocation));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(dstLocation));
        when(articleRepository.findById(100L)).thenReturn(Optional.of(sampleArticle));
        when(stockTransferRepository.countTotalTransfers()).thenReturn(0L);
        when(stockTransferRepository.existsByTransferNumber(any())).thenReturn(false);
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(invocation -> {
            StockTransfer st = invocation.getArgument(0);
            st.setId(1L);
            return st;
        });

        StockTransferRequest request = new StockTransferRequest(
                1L, 10L, 2L, 20L, "Note test",
                List.of(new StockTransferItemRequest(100L, new BigDecimal("10.0000")))
        );

        StockTransferResponse response = stockTransferService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(StockTransferStatus.DRAFT);
        assertThat(response.sourceWarehouseCode()).isEqualTo("WH-MAIN");
        assertThat(response.destinationWarehouseCode()).isEqualTo("WH-TNG");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).articleReference()).isEqualTo("ART-TEST-001");
        assertThat(response.items().get(0).quantity()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Create transfer fails if source warehouse is inactive")
    void testCreate_FailsWhenSourceWarehouseInactive() {
        srcWarehouse.setActive(false);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));

        StockTransferRequest request = new StockTransferRequest(
                1L, null, 2L, null, null,
                List.of(new StockTransferItemRequest(100L, new BigDecimal("5.0000")))
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Source warehouse is inactive");
    }

    @Test
    @DisplayName("Create transfer fails if destination warehouse is inactive")
    void testCreate_FailsWhenDestinationWarehouseInactive() {
        dstWarehouse.setActive(false);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(dstWarehouse));

        StockTransferRequest request = new StockTransferRequest(
                1L, null, 2L, null, null,
                List.of(new StockTransferItemRequest(100L, new BigDecimal("5.0000")))
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Destination warehouse is inactive");
    }

    @Test
    @DisplayName("Create transfer fails if source location does not belong to source warehouse")
    void testCreate_FailsWhenLocationDoesNotBelongToWarehouse() {
        Warehouse otherWh = Warehouse.builder().id(999L).code("WH-OTHER").active(true).build();
        srcLocation.setWarehouse(otherWh);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(dstWarehouse));
        when(warehouseLocationRepository.findById(10L)).thenReturn(Optional.of(srcLocation));

        StockTransferRequest request = new StockTransferRequest(
                1L, 10L, 2L, null, null,
                List.of(new StockTransferItemRequest(100L, new BigDecimal("5.0000")))
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Source location does not belong to source warehouse");
    }

    @Test
    @DisplayName("Create transfer fails if source and destination location are identical")
    void testCreate_FailsWhenSameSourceAndDestinationLocation() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseLocationRepository.findById(10L)).thenReturn(Optional.of(srcLocation));

        StockTransferRequest request = new StockTransferRequest(
                1L, 10L, 1L, 10L, null,
                List.of(new StockTransferItemRequest(100L, new BigDecimal("5.0000")))
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Source and destination location cannot be the same");
    }

    @Test
    @DisplayName("Create transfer fails if items contain duplicate article lines")
    void testCreate_FailsWhenDuplicateArticles() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(dstWarehouse));
        when(warehouseLocationRepository.findById(10L)).thenReturn(Optional.of(srcLocation));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(dstLocation));

        StockTransferRequest request = new StockTransferRequest(
                1L, 10L, 2L, 20L, null,
                List.of(
                        new StockTransferItemRequest(100L, new BigDecimal("5.0000")),
                        new StockTransferItemRequest(100L, new BigDecimal("3.0000"))
                )
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Duplicate article lines are not allowed");
    }

    @Test
    @DisplayName("Create transfer fails if item quantity is zero or negative")
    void testCreate_FailsWhenQuantityZeroOrNegative() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(srcWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(dstWarehouse));
        when(warehouseLocationRepository.findById(10L)).thenReturn(Optional.of(srcLocation));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(dstLocation));

        StockTransferRequest request = new StockTransferRequest(
                1L, 10L, 2L, 20L, null,
                List.of(new StockTransferItemRequest(100L, BigDecimal.ZERO))
        );

        assertThatThrownBy(() -> stockTransferService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Quantity must be strictly positive");
    }

    @Test
    @DisplayName("Complete transfer succeeds: decrements source, increments destination, Article.stockQuantity untouched, movements created")
    void testCompleteTransfer_Success() {
        StockTransfer transfer = StockTransfer.builder()
                .id(10L)
                .transferNumber("TRF-2026-0001")
                .status(StockTransferStatus.DRAFT)
                .sourceWarehouse(srcWarehouse)
                .sourceLocation(srcLocation)
                .destinationWarehouse(dstWarehouse)
                .destinationLocation(dstLocation)
                .items(new ArrayList<>())
                .build();

        StockTransferItem item = StockTransferItem.builder()
                .id(1001L)
                .transfer(transfer)
                .article(sampleArticle)
                .quantity(new BigDecimal("15.0000"))
                .build();
        transfer.addItem(item);

        WarehouseStock srcStock = WarehouseStock.builder()
                .id(501L)
                .article(sampleArticle)
                .warehouse(srcWarehouse)
                .location(srcLocation)
                .quantity(new BigDecimal("40.0000"))
                .build();

        WarehouseStock dstStock = WarehouseStock.builder()
                .id(502L)
                .article(sampleArticle)
                .warehouse(dstWarehouse)
                .location(dstLocation)
                .quantity(new BigDecimal("5.0000"))
                .build();

        when(stockTransferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(transfer));
        when(articleRepository.findAllByIdInForUpdate(List.of(100L))).thenReturn(List.of(sampleArticle));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(100L, 1L, 10L))
                .thenReturn(Optional.of(srcStock));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(100L, 2L, 20L))
                .thenReturn(Optional.of(dstStock));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal initialArticleStock = sampleArticle.getStockQuantity();

        StockTransferResponse response = stockTransferService.complete(10L, sampleUser);

        assertThat(response.status()).isEqualTo(StockTransferStatus.COMPLETED);
        assertThat(response.completedAt()).isNotNull();

        // 1. Source stock decreased by 15 (40 -> 25)
        assertThat(srcStock.getQuantity()).isEqualByComparingTo("25.0000");

        // 2. Destination stock increased by 15 (5 -> 20)
        assertThat(dstStock.getQuantity()).isEqualByComparingTo("20.0000");

        // 3. Article total company-wide stockQuantity remains UNCHANGED
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo(initialArticleStock);
        verify(articleRepository, never()).save(any(Article.class));

        // 4. Exactly 2 StockMovements saved (one OUT, one IN)
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(2)).save(movementCaptor.capture());

        List<StockMovement> savedMovements = movementCaptor.getAllValues();
        StockMovement outMv = savedMovements.stream().filter(m -> m.getType() == StockMovementType.OUT).findFirst().orElseThrow();
        StockMovement inMv = savedMovements.stream().filter(m -> m.getType() == StockMovementType.IN).findFirst().orElseThrow();

        assertThat(outMv.getWarehouse().getId()).isEqualTo(1L);
        assertThat(outMv.getLocation().getId()).isEqualTo(10L);
        assertThat(outMv.getQuantity()).isEqualByComparingTo("15.0000");
        assertThat(outMv.getReference()).isEqualTo("TRF-2026-0001");

        assertThat(inMv.getWarehouse().getId()).isEqualTo(2L);
        assertThat(inMv.getLocation().getId()).isEqualTo(20L);
        assertThat(inMv.getQuantity()).isEqualByComparingTo("15.0000");
        assertThat(inMv.getReference()).isEqualTo("TRF-2026-0001");
    }

    @Test
    @DisplayName("Complete transfer fails when source stock is insufficient")
    void testComplete_FailsWhenInsufficientStock() {
        StockTransfer transfer = StockTransfer.builder()
                .id(10L)
                .transferNumber("TRF-2026-0001")
                .status(StockTransferStatus.DRAFT)
                .sourceWarehouse(srcWarehouse)
                .sourceLocation(srcLocation)
                .destinationWarehouse(dstWarehouse)
                .destinationLocation(dstLocation)
                .items(new ArrayList<>())
                .build();

        transfer.addItem(StockTransferItem.builder()
                .article(sampleArticle)
                .quantity(new BigDecimal("50.0000"))
                .build());

        WarehouseStock srcStock = WarehouseStock.builder()
                .id(501L)
                .article(sampleArticle)
                .warehouse(srcWarehouse)
                .location(srcLocation)
                .quantity(new BigDecimal("20.0000")) // only 20 available, need 50
                .build();

        when(stockTransferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(transfer));
        when(articleRepository.findAllByIdInForUpdate(List.of(100L))).thenReturn(List.of(sampleArticle));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(100L, 1L, 10L))
                .thenReturn(Optional.of(srcStock));

        assertThatThrownBy(() -> stockTransferService.complete(10L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Double completion rejection: cannot complete a transfer that is already COMPLETED")
    void testComplete_FailsWhenAlreadyCompleted() {
        StockTransfer completedTransfer = StockTransfer.builder()
                .id(10L)
                .status(StockTransferStatus.COMPLETED)
                .build();

        when(stockTransferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(completedTransfer));

        assertThatThrownBy(() -> stockTransferService.complete(10L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only DRAFT transfers can be completed");
    }

    @Test
    @DisplayName("Cancel transfer succeeds for DRAFT transfer")
    void testCancelTransfer_Success() {
        StockTransfer draftTransfer = StockTransfer.builder()
                .id(10L)
                .transferNumber("TRF-2026-0001")
                .status(StockTransferStatus.DRAFT)
                .build();

        when(stockTransferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(draftTransfer));
        when(stockTransferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));

        StockTransferResponse response = stockTransferService.cancel(10L, sampleUser);

        assertThat(response.status()).isEqualTo(StockTransferStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel transfer fails if not in DRAFT status")
    void testCancelTransfer_FailsWhenNotDraft() {
        StockTransfer completedTransfer = StockTransfer.builder()
                .id(10L)
                .status(StockTransferStatus.COMPLETED)
                .build();

        when(stockTransferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(completedTransfer));

        assertThatThrownBy(() -> stockTransferService.cancel(10L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only DRAFT transfers can be cancelled");
    }
}
