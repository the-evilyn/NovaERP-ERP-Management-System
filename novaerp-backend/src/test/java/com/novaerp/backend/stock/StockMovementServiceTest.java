package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.stock.dto.StockMovementResponse;
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

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private Article sampleArticle;
    private User testUser;
    private Warehouse defaultWarehouse;
    private WarehouseLocation defaultLocation;
    private WarehouseStock defaultWarehouseStock;

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

        defaultWarehouse = Warehouse.builder()
                .id(1L)
                .code("WH-MAIN")
                .name("Entrepôt Principal Casablanca")
                .active(true)
                .isDefault(true)
                .build();

        defaultLocation = WarehouseLocation.builder()
                .id(1L)
                .warehouse(defaultWarehouse)
                .code("LOC-GEN")
                .name("Zone Générale")
                .active(true)
                .isDefault(true)
                .build();

        defaultWarehouseStock = WarehouseStock.builder()
                .id(1L)
                .article(sampleArticle)
                .warehouse(defaultWarehouse)
                .location(defaultLocation)
                .quantity(new BigDecimal("100.0000"))
                .minQuantity(new BigDecimal("10.0000"))
                .build();

        lenient().when(warehouseRepository.findByCode("WH-MAIN")).thenReturn(Optional.of(defaultWarehouse));
        lenient().when(warehouseLocationRepository.findByWarehouseIdAndCode(1L, "LOC-GEN")).thenReturn(Optional.of(defaultLocation));
        lenient().when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(1L, 1L, 1L)).thenReturn(Optional.of(defaultWarehouseStock));
        lenient().when(warehouseStockRepository.save(any(WarehouseStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("IN movement increases article stock quantity and warehouse stock")
    void testRecordInMovement_IncreasesStock() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("50.0000"), "PO-100", "Inflow"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(StockMovementType.IN);
        assertThat(result.getQuantity()).isEqualByComparingTo("50.0000");
        assertThat(result.getWarehouse()).isEqualTo(defaultWarehouse);
        assertThat(result.getLocation()).isEqualTo(defaultLocation);
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("150.0000");
        assertThat(defaultWarehouseStock.getQuantity()).isEqualByComparingTo("150.0000");

        verify(articleRepository).save(sampleArticle);
        verify(warehouseStockRepository).save(defaultWarehouseStock);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("OUT movement decreases article stock quantity and warehouse stock")
    void testRecordOutMovement_DecreasesStock() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.OUT, new BigDecimal("30.0000"), "SO-200", "Outflow"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(StockMovementType.OUT);
        assertThat(result.getWarehouse()).isEqualTo(defaultWarehouse);
        assertThat(result.getLocation()).isEqualTo(defaultLocation);
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("70.0000");
        assertThat(defaultWarehouseStock.getQuantity()).isEqualByComparingTo("70.0000");

        verify(articleRepository).save(sampleArticle);
        verify(warehouseStockRepository).save(defaultWarehouseStock);
    }

    @Test
    @DisplayName("OUT movement with insufficient stock throws BAD_REQUEST")
    void testRecordOutMovement_InsufficientStock_ThrowsBadRequest() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));

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
    @DisplayName("ADJUSTMENT with positive quantity increases both stocks")
    void testRecordAdjustment_PositiveQuantity_IncreasesStock() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("20.0000"), "ADJ-001", "Count surplus"
        );

        stockMovementService.record(request, testUser);

        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("120.0000");
        assertThat(defaultWarehouseStock.getQuantity()).isEqualByComparingTo("120.0000");
        verify(warehouseStockRepository).save(defaultWarehouseStock);
    }

    @Test
    @DisplayName("ADJUSTMENT with negative quantity decreases both stocks")
    void testRecordAdjustment_NegativeQuantity_DecreasesStock() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("-25.0000"), "ADJ-002", "Defect correction"
        );

        stockMovementService.record(request, testUser);

        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("75.0000");
        assertThat(defaultWarehouseStock.getQuantity()).isEqualByComparingTo("75.0000");
        verify(warehouseStockRepository).save(defaultWarehouseStock);
    }

    @Test
    @DisplayName("ADJUSTMENT driving stock below zero throws BAD_REQUEST")
    void testRecordAdjustment_NegativeQuantityBelowZero_ThrowsBadRequest() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));

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
        when(articleRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

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

    @Test
    @DisplayName("StockMovementRequest 5-arg constructor defaults warehouseId and locationId to null")
    void testStockMovementRequest_FiveArgConstructor_DefaultsWarehouseAndLocationToNull() {
        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "REF-1", "Note"
        );

        assertThat(request.articleId()).isEqualTo(1L);
        assertThat(request.type()).isEqualTo(StockMovementType.IN);
        assertThat(request.quantity()).isEqualByComparingTo("10.0000");
        assertThat(request.reference()).isEqualTo("REF-1");
        assertThat(request.note()).isEqualTo("Note");
        assertThat(request.warehouseId()).isNull();
        assertThat(request.locationId()).isNull();
    }

    @Test
    @DisplayName("StockMovementRequest 7-arg constructor preserves warehouseId and locationId")
    void testStockMovementRequest_SevenArgConstructor_PreservesWarehouseAndLocation() {
        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.OUT, new BigDecimal("5.0000"), "REF-2", "Note 2", 10L, 20L
        );

        assertThat(request.warehouseId()).isEqualTo(10L);
        assertThat(request.locationId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("StockMovementResponse maps warehouse and location metadata when present")
    void testStockMovementResponse_MapsWarehouseAndLocationMetadata() {
        Warehouse wh = Warehouse.builder().id(10L).code("WH-MAIN").name("Entrepôt Principal").build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).code("LOC-GEN").name("Zone Générale").build();

        StockMovement movement = StockMovement.builder()
                .id(100L)
                .article(sampleArticle)
                .type(StockMovementType.IN)
                .quantity(new BigDecimal("25.0000"))
                .reference("REF-WH")
                .note("Warehouse test")
                .warehouse(wh)
                .location(loc)
                .createdBy(testUser)
                .build();

        StockMovementResponse response = StockMovementResponse.from(movement);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.warehouseId()).isEqualTo(10L);
        assertThat(response.warehouseCode()).isEqualTo("WH-MAIN");
        assertThat(response.warehouseName()).isEqualTo("Entrepôt Principal");
        assertThat(response.locationId()).isEqualTo(20L);
        assertThat(response.locationCode()).isEqualTo("LOC-GEN");
        assertThat(response.locationName()).isEqualTo("Zone Générale");
    }

    @Test
    @DisplayName("StockMovementResponse handles null warehouse and location gracefully")
    void testStockMovementResponse_HandlesNullWarehouseAndLocation() {
        StockMovement movement = StockMovement.builder()
                .id(101L)
                .article(sampleArticle)
                .type(StockMovementType.OUT)
                .quantity(new BigDecimal("15.0000"))
                .createdBy(testUser)
                .build();

        StockMovementResponse response = StockMovementResponse.from(movement);

        assertThat(response.warehouseId()).isNull();
        assertThat(response.warehouseCode()).isNull();
        assertThat(response.warehouseName()).isNull();
        assertThat(response.locationId()).isNull();
        assertThat(response.locationCode()).isNull();
        assertThat(response.locationName()).isNull();
    }

    @Test
    @DisplayName("Default WH-MAIN and LOC-GEN fallback is applied when warehouse and location are omitted")
    void testRecordMovement_DefaultWarehouseAndLocationFallback() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-DEF", "Default fallback test"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result.getWarehouse()).isNotNull();
        assertThat(result.getWarehouse().getCode()).isEqualTo("WH-MAIN");
        assertThat(result.getLocation()).isNotNull();
        assertThat(result.getLocation().getCode()).isEqualTo("LOC-GEN");
    }

    @Test
    @DisplayName("Explicit warehouse and location updates specific location stock and stores them in movement")
    void testRecordMovement_ExplicitWarehouseAndLocation() {
        Warehouse wh2 = Warehouse.builder().id(2L).code("WH-TNG").name("Entrepôt Tanger").active(true).build();
        WarehouseLocation loc2 = WarehouseLocation.builder().id(20L).warehouse(wh2).code("LOC-A1").name("Allée A1").active(true).build();
        WarehouseStock ws2 = WarehouseStock.builder().id(200L).article(sampleArticle).warehouse(wh2).location(loc2).quantity(new BigDecimal("30.0000")).build();

        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh2));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc2));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(1L, 2L, 20L)).thenReturn(Optional.of(ws2));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("20.0000"), "PO-TNG", "Tanger stock in", 2L, 20L
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result.getWarehouse().getCode()).isEqualTo("WH-TNG");
        assertThat(result.getLocation().getCode()).isEqualTo("LOC-A1");
        assertThat(ws2.getQuantity()).isEqualByComparingTo("50.0000");
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("120.0000");

        verify(warehouseStockRepository).save(ws2);
        verify(articleRepository).save(sampleArticle);
    }

    @Test
    @DisplayName("OUT rejected when warehouse/location stock is insufficient even if global stock is sufficient")
    void testRecordOutMovement_InsufficientLocationStock_ThrowsBadRequest() {
        Warehouse wh2 = Warehouse.builder().id(2L).code("WH-TNG").name("Entrepôt Tanger").active(true).build();
        WarehouseLocation loc2 = WarehouseLocation.builder().id(20L).warehouse(wh2).code("LOC-A1").name("Allée A1").active(true).build();
        // Location has only 10, but global sampleArticle has 100
        WarehouseStock ws2 = WarehouseStock.builder().id(200L).article(sampleArticle).warehouse(wh2).location(loc2).quantity(new BigDecimal("10.0000")).build();

        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh2));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc2));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(1L, 2L, 20L)).thenReturn(Optional.of(ws2));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.OUT, new BigDecimal("25.0000"), "SO-LOC-ERR", "Exceeds loc stock", 2L, 20L
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Insufficient stock in warehouse location");
                });

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid warehouse ID throws NOT_FOUND")
    void testRecordMovement_InvalidWarehouse_ThrowsNotFound() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-ERR", "Invalid WH", 999L, null
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Warehouse not found");
                });
    }

    @Test
    @DisplayName("Inactive warehouse throws BAD_REQUEST")
    void testRecordMovement_InactiveWarehouse_ThrowsBadRequest() {
        Warehouse inactiveWh = Warehouse.builder().id(3L).code("WH-OLD").name("Ancien Entrepôt").active(false).build();
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(inactiveWh));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-ERR", "Inactive WH", 3L, null
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Warehouse is inactive");
                });
    }

    @Test
    @DisplayName("Invalid location ID throws NOT_FOUND")
    void testRecordMovement_InvalidLocation_ThrowsNotFound() {
        Warehouse wh = Warehouse.builder().id(2L).code("WH-TNG").active(true).build();
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(999L)).thenReturn(Optional.empty());

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-ERR", "Invalid Loc", 2L, 999L
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Location not found");
                });
    }

    @Test
    @DisplayName("Inactive location throws BAD_REQUEST")
    void testRecordMovement_InactiveLocation_ThrowsBadRequest() {
        Warehouse wh = Warehouse.builder().id(2L).code("WH-TNG").active(true).build();
        WarehouseLocation inactiveLoc = WarehouseLocation.builder().id(20L).warehouse(wh).active(false).build();

        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(inactiveLoc));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-ERR", "Inactive Loc", 2L, 20L
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Location is inactive");
                });
    }

    @Test
    @DisplayName("Location belonging to another warehouse is rejected with BAD_REQUEST")
    void testRecordMovement_LocationBelongingToAnotherWarehouse_ThrowsBadRequest() {
        Warehouse wh1 = Warehouse.builder().id(1L).code("WH-MAIN").active(true).build();
        Warehouse otherWh = Warehouse.builder().id(2L).code("WH-OTHER").active(true).build();
        WarehouseLocation locOfOtherWh = WarehouseLocation.builder().id(20L).warehouse(otherWh).active(true).build();

        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(wh1));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(locOfOtherWh));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-ERR", "Mismatch", 1L, 20L
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Location does not belong to specified warehouse");
                });
    }

    @Test
    @DisplayName("ADJUSTMENT driving warehouse stock below zero throws BAD_REQUEST")
    void testRecordAdjustment_NegativeQuantityBelowZeroOnWarehouseStock_ThrowsBadRequest() {
        Warehouse wh2 = Warehouse.builder().id(2L).code("WH-TNG").active(true).build();
        WarehouseLocation loc2 = WarehouseLocation.builder().id(20L).warehouse(wh2).active(true).build();
        // Location has 10, global has 100
        WarehouseStock ws2 = WarehouseStock.builder().id(200L).article(sampleArticle).warehouse(wh2).location(loc2).quantity(new BigDecimal("10.0000")).build();

        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh2));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc2));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(1L, 2L, 20L)).thenReturn(Optional.of(ws2));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.ADJUSTMENT, new BigDecimal("-15.0000"), "ADJ-WH-ERR", "Over correction on WH", 2L, 20L
        );

        assertThatThrownBy(() -> stockMovementService.record(request, testUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Warehouse stock quantity cannot go below zero");
                });
    }

    @Test
    @DisplayName("Creates WarehouseStock with quantity 0 if none exists before applying movement")
    void testRecordMovement_CreatesWarehouseStockIfNoneExists() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(1L, 1L, 1L)).thenReturn(Optional.empty());
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("15.0000"), "PO-NEW", "First time stock"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        assertThat(sampleArticle.getStockQuantity()).isEqualByComparingTo("115.0000");
        verify(warehouseStockRepository).save(argThat(ws ->
                ws.getQuantity().compareTo(new BigDecimal("15.0000")) == 0 &&
                ws.getWarehouse().getId().equals(1L) &&
                ws.getLocation().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("StockMovementService uses findByIdForUpdate to lock article with PESSIMISTIC_WRITE before stock validation")
    void testRecordMovement_UsesFindByIdForUpdate_LocksArticle() {
        when(articleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleArticle));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockMovementRequest request = new StockMovementRequest(
                1L, StockMovementType.IN, new BigDecimal("10.0000"), "PO-LOCK", "Lock verification"
        );

        StockMovement result = stockMovementService.record(request, testUser);

        assertThat(result).isNotNull();
        verify(articleRepository).findByIdForUpdate(1L);
        verify(articleRepository, never()).findById(any());
    }
}
