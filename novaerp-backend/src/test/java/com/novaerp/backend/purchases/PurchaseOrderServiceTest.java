package com.novaerp.backend.purchases;

import com.novaerp.backend.purchases.dto.PurchaseOrderItemRequest;
import com.novaerp.backend.purchases.dto.PurchaseOrderRequest;
import com.novaerp.backend.purchases.dto.PurchaseOrderResponse;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
import com.novaerp.backend.stock.StockMovementService;
import com.novaerp.backend.stock.StockMovementType;
import com.novaerp.backend.stock.*;
import com.novaerp.backend.stock.dto.StockMovementRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier sampleSupplier;
    private Article sampleArticle;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleSupplier = Supplier.builder()
                .id(1L)
                .name("Industrie Outillage Maroc")
                .email("contact@outillage.ma")
                .build();

        sampleArticle = Article.builder()
                .id(10L)
                .reference("ART-ROUL-001")
                .designation("Roulement à billes 6204")
                .stockQuantity(new BigDecimal("20.0000"))
                .purchasePriceHt(new BigDecimal("40.0000"))
                .build();

        sampleUser = User.builder()
                .id(99L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Create valid purchase order calculates totals and sets DRAFT status")
    void testCreatePurchaseOrder_Success() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("50.0000"),
                new BigDecimal("40.0000"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Commande appro");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(purchaseOrderRepository.countTotalOrders()).thenReturn(0L);
        when(purchaseOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        PurchaseOrderResponse response = purchaseOrderService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).startsWith("PO-");
        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("2000.0000"));
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("400.0000"));
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("2400.0000"));
        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("Create purchase order with non-existent supplier throws 404")
    void testCreatePurchaseOrder_SupplierNotFound() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(10L, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);
        PurchaseOrderRequest request = new PurchaseOrderRequest(999L, List.of(itemReq), null, null);

        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Confirm moves purchase order from DRAFT to CONFIRMED without stock movements yet")
    void testConfirm_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.confirm(100L, sampleUser);

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        verifyNoInteractions(stockMovementService);
    }

    @Test
    @DisplayName("Confirm already confirmed order throws 400")
    void testConfirm_AlreadyConfirmed() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .status(PurchaseOrderStatus.CONFIRMED)
                .build();
        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Receive confirmed order transactionally records IN stock movements")
    void testReceive_Success() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("30.0000"))
                .unitPrice(new BigDecimal("40.0000"))
                .totalHt(new BigDecimal("1200.0000"))
                .totalTtc(new BigDecimal("1440.0000"))
                .build();
        order.addItem(item);

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.receive(100L, sampleUser);

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(response.receivedAt()).isNotNull();

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService, times(1)).record(captor.capture(), eq(sampleUser));

        StockMovementRequest mvt = captor.getValue();
        assertThat(mvt.articleId()).isEqualTo(10L);
        assertThat(mvt.type()).isEqualTo(StockMovementType.IN);
        assertThat(mvt.quantity()).isEqualByComparingTo(new BigDecimal("30.0000"));
        assertThat(mvt.reference()).isEqualTo("PO-2026-0001");
        assertThat(mvt.warehouseId()).isNull();
        assertThat(mvt.locationId()).isNull();
    }

    @Test
    @DisplayName("Receive draft order throws 400")
    void testReceive_DraftOrderThrows() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .status(PurchaseOrderStatus.DRAFT)
                .build();
        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.receive(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Cancel draft or confirmed purchase order succeeds")
    void testCancel_DraftSuccess() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        verifyNoInteractions(stockMovementService);
    }

    @Test
    @DisplayName("Cancel already received order throws 400")
    void testCancel_ReceivedThrows() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .status(PurchaseOrderStatus.RECEIVED)
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> purchaseOrderService.cancel(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("PurchaseOrderResponse.from maps null warehouse and location safely")
    void testPurchaseOrderResponse_NullWarehouseAndLocation() {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        PurchaseOrderResponse response = PurchaseOrderResponse.from(order);

        assertThat(response.warehouseId()).isNull();
        assertThat(response.warehouseCode()).isNull();
        assertThat(response.warehouseName()).isNull();
        assertThat(response.locationId()).isNull();
        assertThat(response.locationCode()).isNull();
        assertThat(response.locationName()).isNull();
    }

    @Test
    @DisplayName("PurchaseOrderResponse.from correctly exposes warehouse and location when present")
    void testPurchaseOrderResponse_WithWarehouseAndLocation() {
        Warehouse wh = Warehouse.builder().id(3L).code("WH-SOUTH").name("Entrepôt Sud").build();
        WarehouseLocation loc = WarehouseLocation.builder().id(30L).code("LOC-B2").name("Zone B2").warehouse(wh).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        PurchaseOrderResponse response = PurchaseOrderResponse.from(order);

        assertThat(response.warehouseId()).isEqualTo(3L);
        assertThat(response.warehouseCode()).isEqualTo("WH-SOUTH");
        assertThat(response.warehouseName()).isEqualTo("Entrepôt Sud");
        assertThat(response.locationId()).isEqualTo(30L);
        assertThat(response.locationCode()).isEqualTo("LOC-B2");
        assertThat(response.locationName()).isEqualTo("Zone B2");
    }

    @Test
    @DisplayName("PurchaseOrderRequest supports nullable warehouseId and locationId")
    void testPurchaseOrderRequest_WarehouseAndLocationFields() {
        PurchaseOrderRequest requestWithNulls = new PurchaseOrderRequest(1L, List.of(), new BigDecimal("20.00"), "Notes");
        assertThat(requestWithNulls.warehouseId()).isNull();
        assertThat(requestWithNulls.locationId()).isNull();

        PurchaseOrderRequest requestWithValues = new PurchaseOrderRequest(1L, List.of(), new BigDecimal("20.00"), "Notes", 7L, 70L);
        assertThat(requestWithValues.warehouseId()).isEqualTo(7L);
        assertThat(requestWithValues.locationId()).isEqualTo(70L);
    }

    @Test
    @DisplayName("Receive confirmed order with warehouse and location records IN movement with warehouseId and locationId")
    void testReceive_WithValidWarehouseAndLocation_Success() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Allée A1").warehouse(wh).active(true).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("30.0000"))
                .unitPrice(new BigDecimal("40.0000"))
                .totalHt(new BigDecimal("1200.0000"))
                .totalTtc(new BigDecimal("1440.0000"))
                .build();
        order.addItem(item);

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.receive(100L, sampleUser);

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(response.warehouseId()).isEqualTo(2L);
        assertThat(response.locationId()).isEqualTo(20L);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService, times(1)).record(captor.capture(), eq(sampleUser));

        StockMovementRequest mvt = captor.getValue();
        assertThat(mvt.articleId()).isEqualTo(10L);
        assertThat(mvt.type()).isEqualTo(StockMovementType.IN);
        assertThat(mvt.quantity()).isEqualByComparingTo(new BigDecimal("30.0000"));
        assertThat(mvt.reference()).isEqualTo("PO-2026-0001");
        assertThat(mvt.warehouseId()).isEqualTo(2L);
        assertThat(mvt.locationId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Receive order with inactive warehouse throws 400 BAD_REQUEST")
    void testReceive_RejectsInactiveWarehouse() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Inactif").active(false).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .warehouse(wh)
                .items(new ArrayList<>())
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));

        assertThatThrownBy(() -> purchaseOrderService.receive(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("est inactif");
                });

        verifyNoInteractions(stockMovementService);
    }

    @Test
    @DisplayName("Receive order with inactive location throws 400 BAD_REQUEST")
    void testReceive_RejectsInactiveLocation() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Zone Inactive").warehouse(wh).active(false).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));

        assertThatThrownBy(() -> purchaseOrderService.receive(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("est inactif");
                });

        verifyNoInteractions(stockMovementService);
    }

    @Test
    @DisplayName("Receive order with location belonging to another warehouse throws 400 BAD_REQUEST")
    void testReceive_RejectsLocationBelongingToAnotherWarehouse() {
        Warehouse wh1 = Warehouse.builder().id(2L).name("Entrepôt 1").active(true).build();
        Warehouse wh2 = Warehouse.builder().id(3L).name("Entrepôt 2").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Zone A").warehouse(wh2).active(true).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .warehouse(wh1)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh1));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));

        assertThatThrownBy(() -> purchaseOrderService.receive(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("n'appartient pas");
                });

        verifyNoInteractions(stockMovementService);
    }

    @Test
    @DisplayName("Receive order with warehouse but null location resolves default active location")
    void testReceive_WithWarehouseAndNullLocation_ResolvesDefaultLocation() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation defaultLoc = WarehouseLocation.builder().id(25L).code("LOC-GEN").name("Général").warehouse(wh).active(true).build();

        PurchaseOrder order = PurchaseOrder.builder()
                .id(100L)
                .orderNumber("PO-2026-0001")
                .supplier(sampleSupplier)
                .status(PurchaseOrderStatus.CONFIRMED)
                .warehouse(wh)
                .location(null)
                .items(new ArrayList<>())
                .build();

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .id(1L)
                .purchaseOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("15.0000"))
                .unitPrice(new BigDecimal("40.0000"))
                .totalHt(new BigDecimal("600.0000"))
                .totalTtc(new BigDecimal("720.0000"))
                .build();
        order.addItem(item);

        when(purchaseOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(2L, "LOC-GEN")).thenReturn(Optional.of(defaultLoc));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrderResponse response = purchaseOrderService.receive(100L, sampleUser);

        assertThat(response.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(order.getLocation()).isEqualTo(defaultLoc);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService, times(1)).record(captor.capture(), eq(sampleUser));

        StockMovementRequest mvt = captor.getValue();
        assertThat(mvt.type()).isEqualTo(StockMovementType.IN);
        assertThat(mvt.warehouseId()).isEqualTo(2L);
        assertThat(mvt.locationId()).isEqualTo(25L);
    }

    @Test
    @DisplayName("Create purchase order with warehouse and location sets them on entity")
    void testCreatePurchaseOrder_WithWarehouseAndLocation() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Allée A1").warehouse(wh).active(true).build();

        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("10.0000"),
                new BigDecimal("40.0000"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Notes", 2L, 20L);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));
        when(purchaseOrderRepository.countTotalOrders()).thenReturn(0L);
        when(purchaseOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        PurchaseOrderResponse response = purchaseOrderService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.warehouseId()).isEqualTo(2L);
        assertThat(response.locationId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Create purchase order with location but no warehouse throws 400 BAD_REQUEST")
    void testCreatePurchaseOrder_RejectsLocationWithoutWarehouse() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("10.0000"),
                new BigDecimal("40.0000"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Notes", null, 20L);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        assertThatThrownBy(() -> purchaseOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Un entrepôt doit être spécifié");
                });
    }

    @Test
    @DisplayName("Create purchase order with inactive warehouse throws 400 BAD_REQUEST")
    void testCreatePurchaseOrder_RejectsInactiveWarehouse() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Inactif").active(false).build();

        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("10.0000"),
                new BigDecimal("40.0000"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Notes", 2L, null);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));

        assertThatThrownBy(() -> purchaseOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("est inactif");
                });
    }
}
