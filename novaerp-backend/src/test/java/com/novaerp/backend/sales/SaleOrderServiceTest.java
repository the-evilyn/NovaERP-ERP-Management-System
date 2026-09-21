package com.novaerp.backend.sales;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.sales.dto.SaleOrderItemRequest;
import com.novaerp.backend.sales.dto.SaleOrderRequest;
import com.novaerp.backend.sales.dto.SaleOrderResponse;
import com.novaerp.backend.stock.Article;
import com.novaerp.backend.stock.ArticleRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleOrderServiceTest {

    @Mock
    private SaleOrderRepository saleOrderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @InjectMocks
    private SaleOrderService saleOrderService;

    private Client sampleClient;
    private Article sampleArticle;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleClient = Client.builder()
                .id(1L)
                .name("Maroc Métal Industrie")
                .city("Casablanca")
                .build();

        sampleArticle = Article.builder()
                .id(10L)
                .reference("ART-ROUL-001")
                .designation("Roulement à billes 6204")
                .stockQuantity(new BigDecimal("100.0000"))
                .salePriceHt(new BigDecimal("50.0000"))
                .build();

        sampleUser = User.builder()
                .id(99L)
                .email("admin@novaerp.local")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    @DisplayName("Create valid sale order calculates totals and sets DRAFT status")
    void testCreateSaleOrder_Success() {
        SaleOrderItemRequest itemReq = new SaleOrderItemRequest(
                10L,
                new BigDecimal("5.0000"),
                new BigDecimal("50.0000"),
                new BigDecimal("20.00")
        );
        SaleOrderRequest request = new SaleOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Commande test");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));
        when(saleOrderRepository.countTotalOrders()).thenReturn(0L);
        when(saleOrderRepository.existsByOrderNumber(anyString())).thenReturn(false);
        when(saleOrderRepository.save(any(SaleOrder.class))).thenAnswer(inv -> {
            SaleOrder order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        SaleOrderResponse response = saleOrderService.create(request, sampleUser);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).contains("SO-");
        assertThat(response.status()).isEqualTo(SaleOrderStatus.DRAFT);
        assertThat(response.clientName()).isEqualTo("Maroc Métal Industrie");
        assertThat(response.items()).hasSize(1);

        // Subtotal HT: 5 * 50 = 250.00
        assertThat(response.subtotalHt()).isEqualByComparingTo(new BigDecimal("250.0000"));
        // Tax 20%: 250 * 0.20 = 50.00
        assertThat(response.taxAmount()).isEqualByComparingTo(new BigDecimal("50.0000"));
        // Total TTC: 300.00
        assertThat(response.totalTtc()).isEqualByComparingTo(new BigDecimal("300.0000"));
    }

    @Test
    @DisplayName("Create sale order with unknown client throws 404 NOT_FOUND")
    void testCreateSaleOrder_ClientNotFound() {
        SaleOrderRequest request = new SaleOrderRequest(999L, List.of(
                new SaleOrderItemRequest(10L, BigDecimal.ONE, BigDecimal.TEN, null)
        ), null, null);

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Create sale order with unknown article throws 404 NOT_FOUND")
    void testCreateSaleOrder_ArticleNotFound() {
        SaleOrderRequest request = new SaleOrderRequest(1L, List.of(
                new SaleOrderItemRequest(999L, BigDecimal.ONE, BigDecimal.TEN, null)
        ), null, null);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Create sale order with zero or negative quantity throws 400 BAD_REQUEST")
    void testCreateSaleOrder_InvalidQuantity() {
        SaleOrderRequest request = new SaleOrderRequest(1L, List.of(
                new SaleOrderItemRequest(10L, BigDecimal.ZERO, BigDecimal.TEN, null)
        ), null, null);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(sampleClient));
        when(articleRepository.findById(10L)).thenReturn(Optional.of(sampleArticle));

        assertThatThrownBy(() -> saleOrderService.create(request, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Confirm order successfully decrements stock via StockMovementService")
    void testConfirmSaleOrder_Success() {
        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle) // Stock: 100
                .quantity(new BigDecimal("20.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1000.0000"))
                .totalTtc(new BigDecimal("1200.0000"))
                .build();
        order.addItem(item);

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.confirm(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();

        // Verify StockMovementService was invoked with OUT movement
        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest movement = captor.getValue();
        assertThat(movement.articleId()).isEqualTo(10L);
        assertThat(movement.type()).isEqualTo(StockMovementType.OUT);
        assertThat(movement.quantity()).isEqualByComparingTo(new BigDecimal("20.0000"));
        assertThat(movement.reference()).isEqualTo("SO-2026-0001");
    }

    @Test
    @DisplayName("Confirm order with insufficient stock throws 400 and does NOT record movements")
    void testConfirmSaleOrder_InsufficientStock() {
        sampleArticle.setStockQuantity(new BigDecimal("5.0000")); // Only 5 in stock

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("20.0000")) // Requesting 20
                .unitPrice(new BigDecimal("50.0000"))
                .totalHt(new BigDecimal("1000.0000"))
                .totalTtc(new BigDecimal("1200.0000"))
                .build();
        order.addItem(item);

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> saleOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Stock insuffisant");
                });

        // Verify no movements recorded
        verify(stockMovementService, never()).record(any(), any());
        assertThat(order.getStatus()).isEqualTo(SaleOrderStatus.DRAFT);
    }

    @Test
    @DisplayName("Cancel DRAFT order sets status CANCELLED without movements")
    void testCancelDraftOrder() {
        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CANCELLED);
        verify(stockMovementService, never()).record(any(), any());
    }

    @Test
    @DisplayName("Cancel CONFIRMED order triggers IN restocking movements")
    void testCancelConfirmedOrder_Restocks() {
        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("15.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .totalHt(new BigDecimal("750.0000"))
                .totalTtc(new BigDecimal("900.0000"))
                .build();
        order.addItem(item);

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CANCELLED);

        // Verify restocking IN movement was recorded
        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest restock = captor.getValue();
        assertThat(restock.type()).isEqualTo(StockMovementType.IN);
        assertThat(restock.quantity()).isEqualByComparingTo(new BigDecimal("15.0000"));
        assertThat(restock.reference()).contains("ANNUL-SO-2026-0001");
    }

    @Test
    @DisplayName("SaleOrderResponse.from maps null warehouse and location safely")
    void testSaleOrderResponse_NullWarehouseAndLocation() {
        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        SaleOrderResponse response = SaleOrderResponse.from(order);

        assertThat(response.warehouseId()).isNull();
        assertThat(response.warehouseCode()).isNull();
        assertThat(response.warehouseName()).isNull();
        assertThat(response.locationId()).isNull();
        assertThat(response.locationCode()).isNull();
        assertThat(response.locationName()).isNull();
    }

    @Test
    @DisplayName("SaleOrderResponse.from correctly exposes warehouse and location when present")
    void testSaleOrderResponse_WithWarehouseAndLocation() {
        Warehouse wh = Warehouse.builder().id(2L).code("WH-NORTH").name("Entrepôt Nord").build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).code("LOC-A1").name("Allée A1").warehouse(wh).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        SaleOrderResponse response = SaleOrderResponse.from(order);

        assertThat(response.warehouseId()).isEqualTo(2L);
        assertThat(response.warehouseCode()).isEqualTo("WH-NORTH");
        assertThat(response.warehouseName()).isEqualTo("Entrepôt Nord");
        assertThat(response.locationId()).isEqualTo(20L);
        assertThat(response.locationCode()).isEqualTo("LOC-A1");
        assertThat(response.locationName()).isEqualTo("Allée A1");
    }

    @Test
    @DisplayName("SaleOrderRequest supports nullable warehouseId and locationId")
    void testSaleOrderRequest_WarehouseAndLocationFields() {
        SaleOrderRequest requestWithNulls = new SaleOrderRequest(1L, List.of(), new BigDecimal("20.00"), "Notes");
        assertThat(requestWithNulls.warehouseId()).isNull();
        assertThat(requestWithNulls.locationId()).isNull();

        SaleOrderRequest requestWithValues = new SaleOrderRequest(1L, List.of(), new BigDecimal("20.00"), "Notes", 5L, 50L);
        assertThat(requestWithValues.warehouseId()).isEqualTo(5L);
        assertThat(requestWithValues.locationId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Confirm order with valid warehouse and location passes and records movements with warehouseId and locationId")
    void testConfirmSaleOrder_WithValidWarehouseAndLocation() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Allée A1").warehouse(wh).active(true).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("10.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("500.0000"))
                .totalTtc(new BigDecimal("600.0000"))
                .build();
        order.addItem(item);

        WarehouseStock ws = WarehouseStock.builder()
                .id(1L)
                .article(sampleArticle)
                .warehouse(wh)
                .location(loc)
                .quantity(new BigDecimal("30.0000"))
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(10L, 2L, 20L))
                .thenReturn(Optional.of(ws));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.confirm(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CONFIRMED);
        assertThat(response.warehouseId()).isEqualTo(2L);
        assertThat(response.locationId()).isEqualTo(20L);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest movement = captor.getValue();
        assertThat(movement.articleId()).isEqualTo(10L);
        assertThat(movement.type()).isEqualTo(StockMovementType.OUT);
        assertThat(movement.quantity()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(movement.warehouseId()).isEqualTo(2L);
        assertThat(movement.locationId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Confirm order with warehouse but null location resolves default active location")
    void testConfirmSaleOrder_ResolvesDefaultLocationWhenNull() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation defaultLoc = WarehouseLocation.builder().id(25L).code("LOC-GEN").name("Général").warehouse(wh).active(true).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(null)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("5.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("250.0000"))
                .totalTtc(new BigDecimal("300.0000"))
                .build();
        order.addItem(item);

        WarehouseStock ws = WarehouseStock.builder()
                .id(1L)
                .article(sampleArticle)
                .warehouse(wh)
                .location(defaultLoc)
                .quantity(new BigDecimal("15.0000"))
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(2L, "LOC-GEN")).thenReturn(Optional.of(defaultLoc));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(10L, 2L, 25L))
                .thenReturn(Optional.of(ws));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.confirm(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CONFIRMED);
        assertThat(order.getLocation()).isEqualTo(defaultLoc);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest movement = captor.getValue();
        assertThat(movement.warehouseId()).isEqualTo(2L);
        assertThat(movement.locationId()).isEqualTo(25L);
    }

    @Test
    @DisplayName("Confirm order with inactive warehouse throws 400 BAD_REQUEST")
    void testConfirmSaleOrder_RejectsInactiveWarehouse() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Inactif").active(false).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));

        assertThatThrownBy(() -> saleOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("est inactif");
                });

        verify(stockMovementService, never()).record(any(), any());
    }

    @Test
    @DisplayName("Confirm order with inactive location throws 400 BAD_REQUEST")
    void testConfirmSaleOrder_RejectsInactiveLocation() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Zone Inactive").warehouse(wh).active(false).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));

        assertThatThrownBy(() -> saleOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("est inactif");
                });

        verify(stockMovementService, never()).record(any(), any());
    }

    @Test
    @DisplayName("Confirm order with location belonging to another warehouse throws 400 BAD_REQUEST")
    void testConfirmSaleOrder_RejectsLocationBelongingToAnotherWarehouse() {
        Warehouse wh1 = Warehouse.builder().id(2L).name("Entrepôt 1").active(true).build();
        Warehouse wh2 = Warehouse.builder().id(3L).name("Entrepôt 2").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Zone A").warehouse(wh2).active(true).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh1)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh1));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));

        assertThatThrownBy(() -> saleOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("n'appartient pas");
                });

        verify(stockMovementService, never()).record(any(), any());
    }

    @Test
    @DisplayName("Confirm order succeeds when WarehouseStock is sufficient even if Article global stock is lower")
    void testConfirmSaleOrder_SucceedsWhenWarehouseStockSufficientEvenIfGlobalStockLower() {
        // Article global stock = 20
        sampleArticle.setStockQuantity(new BigDecimal("20.0000"));

        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Allée A1").warehouse(wh).active(true).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        // Order quantity = 25
        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("25.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1250.0000"))
                .totalTtc(new BigDecimal("1500.0000"))
                .build();
        order.addItem(item);

        // Selected WarehouseStock = 30
        WarehouseStock ws = WarehouseStock.builder()
                .id(1L)
                .article(sampleArticle)
                .warehouse(wh)
                .location(loc)
                .quantity(new BigDecimal("30.0000"))
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(10L, 2L, 20L))
                .thenReturn(Optional.of(ws));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.confirm(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CONFIRMED);
        assertThat(response.warehouseId()).isEqualTo(2L);
        assertThat(response.locationId()).isEqualTo(20L);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest movement = captor.getValue();
        assertThat(movement.articleId()).isEqualTo(10L);
        assertThat(movement.type()).isEqualTo(StockMovementType.OUT);
        assertThat(movement.quantity()).isEqualByComparingTo(new BigDecimal("25.0000"));
        assertThat(movement.warehouseId()).isEqualTo(2L);
        assertThat(movement.locationId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("Confirm order with insufficient WarehouseStock throws 400 BAD_REQUEST")
    void testConfirmSaleOrder_RejectsInsufficientWarehouseStock() {
        Warehouse wh = Warehouse.builder().id(2L).name("Entrepôt Nord").active(true).build();
        WarehouseLocation loc = WarehouseLocation.builder().id(20L).name("Zone A").warehouse(wh).active(true).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        // Order = 30
        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("30.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("1500.0000"))
                .totalTtc(new BigDecimal("1800.0000"))
                .build();
        order.addItem(item);

        // WarehouseStock = 20
        WarehouseStock ws = WarehouseStock.builder()
                .id(1L)
                .article(sampleArticle)
                .warehouse(wh)
                .location(loc)
                .quantity(new BigDecimal("20.0000"))
                .build();

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh));
        when(warehouseLocationRepository.findById(20L)).thenReturn(Optional.of(loc));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(10L, 2L, 20L))
                .thenReturn(Optional.of(ws));

        assertThatThrownBy(() -> saleOrderService.confirm(100L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("Stock insuffisant");
                });

        verify(stockMovementService, never()).record(any(), any());
    }

    @Test
    @DisplayName("Cancel CONFIRMED order with warehouse and location restocks to the exact same warehouse and location")
    void testCancelConfirmedOrder_WithWarehouseAndLocation_RestocksToSameLocation() {
        Warehouse wh = Warehouse.builder().id(3L).name("Entrepôt Sud").build();
        WarehouseLocation loc = WarehouseLocation.builder().id(30L).name("Zone B2").warehouse(wh).build();

        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .warehouse(wh)
                .location(loc)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("15.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .totalHt(new BigDecimal("750.0000"))
                .totalTtc(new BigDecimal("900.0000"))
                .build();
        order.addItem(item);

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CANCELLED);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest restock = captor.getValue();
        assertThat(restock.type()).isEqualTo(StockMovementType.IN);
        assertThat(restock.quantity()).isEqualByComparingTo(new BigDecimal("15.0000"));
        assertThat(restock.reference()).contains("ANNUL-SO-2026-0001");
        assertThat(restock.warehouseId()).isEqualTo(3L);
        assertThat(restock.locationId()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Cancel CONFIRMED legacy order with null warehouse/location preserves fallback behavior")
    void testCancelConfirmedOrder_LegacyOrder_PreservesFallback() {
        SaleOrder order = SaleOrder.builder()
                .id(100L)
                .orderNumber("SO-2026-0001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .warehouse(null)
                .location(null)
                .items(new ArrayList<>())
                .build();

        SaleOrderItem item = SaleOrderItem.builder()
                .id(1L)
                .saleOrder(order)
                .article(sampleArticle)
                .quantity(new BigDecimal("15.0000"))
                .unitPrice(new BigDecimal("50.0000"))
                .totalHt(new BigDecimal("750.0000"))
                .totalTtc(new BigDecimal("900.0000"))
                .build();
        order.addItem(item);

        when(saleOrderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenReturn(order);

        SaleOrderResponse response = saleOrderService.cancel(100L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.CANCELLED);

        ArgumentCaptor<StockMovementRequest> captor = ArgumentCaptor.forClass(StockMovementRequest.class);
        verify(stockMovementService).record(captor.capture(), eq(sampleUser));

        StockMovementRequest restock = captor.getValue();
        assertThat(restock.type()).isEqualTo(StockMovementType.IN);
        assertThat(restock.warehouseId()).isNull();
        assertThat(restock.locationId()).isNull();
    }

    @Test
    @DisplayName("list with combined status and clientId calls findWithFilters")
    void testList_WithBothStatusAndClientId() {
        Pageable pageable = PageRequest.of(0, 10);
        SaleOrder order = SaleOrder.builder()
                .id(1L)
                .orderNumber("CMD-2026-00001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .build();
        Page<SaleOrder> page = new PageImpl<>(List.of(order), pageable, 1);

        when(saleOrderRepository.findWithFilters(SaleOrderStatus.CONFIRMED, 1L, pageable)).thenReturn(page);

        Page<SaleOrderResponse> result = saleOrderService.list(SaleOrderStatus.CONFIRMED, 1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(saleOrderRepository).findWithFilters(SaleOrderStatus.CONFIRMED, 1L, pageable);
    }

    @Test
    @DisplayName("deliver transitions CONFIRMED order to DELIVERED with deliveredAt timestamp")
    void testDeliver_Success() {
        SaleOrder order = SaleOrder.builder()
                .id(1L)
                .orderNumber("CMD-2026-00001")
                .client(sampleClient)
                .status(SaleOrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(saleOrderRepository.save(any(SaleOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleOrderResponse response = saleOrderService.deliver(1L, sampleUser);

        assertThat(response.status()).isEqualTo(SaleOrderStatus.DELIVERED);
        assertThat(response.deliveredAt()).isNotNull();
        verify(saleOrderRepository).save(order);
    }

    @Test
    @DisplayName("deliver rejects order in DRAFT status")
    void testDeliver_RejectsDraft() {
        SaleOrder order = SaleOrder.builder()
                .id(1L)
                .orderNumber("CMD-2026-00001")
                .client(sampleClient)
                .status(SaleOrderStatus.DRAFT)
                .items(new ArrayList<>())
                .build();

        when(saleOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> saleOrderService.deliver(1L, sampleUser))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("doit être confirmée avant d'être livrée");
                });
    }
}
