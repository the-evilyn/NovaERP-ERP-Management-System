package com.novaerp.backend.config;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.invoices.CustomerInvoice;
import com.novaerp.backend.invoices.CustomerInvoiceRepository;
import com.novaerp.backend.invoices.CustomerInvoiceStatus;
import com.novaerp.backend.payments.Payment;
import com.novaerp.backend.payments.PaymentRepository;
import com.novaerp.backend.payments.PaymentType;
import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderRepository;
import com.novaerp.backend.purchases.PurchaseOrderStatus;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.sales.SaleOrderStatus;
import com.novaerp.backend.stock.*;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleSupplierPriceRepository articleSupplierPriceRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SaleOrderRepository saleOrderRepository;

    @Mock
    private CustomerInvoiceRepository customerInvoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<List<WarehouseStock>> warehouseStockListCaptor;

    @Captor
    private ArgumentCaptor<List<StockMovement>> stockMovementListCaptor;

    @Captor
    private ArgumentCaptor<List<Client>> clientListCaptor;

    @Captor
    private ArgumentCaptor<List<PurchaseOrder>> purchaseOrderListCaptor;

    @Captor
    private ArgumentCaptor<List<SaleOrder>> saleOrderListCaptor;

    @Captor
    private ArgumentCaptor<List<CustomerInvoice>> customerInvoiceListCaptor;

    @Captor
    private ArgumentCaptor<List<Payment>> paymentListCaptor;

    @Captor
    private ArgumentCaptor<StockTransfer> stockTransferCaptor;

    private Warehouse defaultWarehouse;
    private WarehouseLocation defaultLocation;
    private Warehouse secondaryWarehouse;
    private WarehouseLocation secondaryLocation;

    @BeforeEach
    void setUp() {
        defaultWarehouse = Warehouse.builder()
                .id(1L)
                .code("WH-MAIN")
                .name("Entrepôt Principal (Siège)")
                .active(true)
                .isDefault(true)
                .build();

        defaultLocation = WarehouseLocation.builder()
                .id(10L)
                .warehouse(defaultWarehouse)
                .code("LOC-GEN")
                .name("Zone Générale")
                .active(true)
                .isDefault(true)
                .build();

        secondaryWarehouse = Warehouse.builder()
                .id(2L)
                .code("WH-NORTH")
                .name("Entrepôt Régional Tanger")
                .active(true)
                .isDefault(false)
                .build();

        secondaryLocation = WarehouseLocation.builder()
                .id(12L)
                .warehouse(secondaryWarehouse)
                .code("LOC-NORTH-01")
                .name("Zone Principale Tanger")
                .active(true)
                .isDefault(true)
                .build();
    }

    @Test
    @DisplayName("When users already exist, DataSeeder skips all seeding")
    void testRun_WhenUsersExist_SkipsSeeding() {
        when(userRepository.count()).thenReturn(3L);

        dataSeeder.run();

        verify(userRepository, never()).saveAll(any());
        verify(articleRepository, never()).saveAll(any());
        verify(warehouseStockRepository, never()).saveAll(any());
        verify(stockMovementRepository, never()).saveAll(any());
        verify(clientRepository, never()).saveAll(any());
        verify(purchaseOrderRepository, never()).saveAll(any());
        verify(saleOrderRepository, never()).saveAll(any());
        verify(customerInvoiceRepository, never()).saveAll(any());
        verify(paymentRepository, never()).saveAll(any());
        verify(stockTransferRepository, never()).save(any());
    }

    @Test
    @DisplayName("On fresh DB, DataSeeder seeds warehouse_stocks matching article quantities and links movements")
    void testRun_OnFreshDatabase_SeedsWarehouseStocksAndLinksMovements() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(unitRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(supplierRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(articleRepository.saveAll(any())).thenAnswer(inv -> {
            List<Article> articles = inv.getArgument(0);
            long id = 1L;
            for (Article a : articles) {
                a.setId(id++);
            }
            return articles;
        });

        when(warehouseRepository.findByCode("WH-MAIN")).thenReturn(Optional.of(defaultWarehouse));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(1L, "LOC-GEN")).thenReturn(Optional.of(defaultLocation));
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(any(), eq(1L), eq(10L)))
                .thenReturn(Optional.empty());

        when(purchaseOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerInvoiceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(warehouseRepository.findByCode("WH-NORTH")).thenReturn(Optional.of(secondaryWarehouse));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(2L, "LOC-NORTH-01")).thenReturn(Optional.of(secondaryLocation));
        when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        verify(warehouseStockRepository).saveAll(warehouseStockListCaptor.capture());
        List<WarehouseStock> savedStocks = warehouseStockListCaptor.getValue();
        assertThat(savedStocks).hasSize(10);

        for (WarehouseStock stock : savedStocks) {
            assertThat(stock.getWarehouse()).isEqualTo(defaultWarehouse);
            assertThat(stock.getLocation()).isEqualTo(defaultLocation);
            assertThat(stock.getArticle()).isNotNull();
            assertThat(stock.getQuantity()).isEqualByComparingTo(stock.getArticle().getStockQuantity());
            assertThat(stock.getMinQuantity()).isEqualByComparingTo(stock.getArticle().getMinStockQuantity());
        }

        BigDecimal totalStockQuantity = savedStocks.stream()
                .map(WarehouseStock::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalStockQuantity).isEqualByComparingTo("1268.0000");

        verify(stockMovementRepository, atLeastOnce()).saveAll(stockMovementListCaptor.capture());
        List<List<StockMovement>> movementBatches = stockMovementListCaptor.getAllValues();
        assertThat(movementBatches).isNotEmpty();
        List<StockMovement> initialMovements = movementBatches.get(0);
        assertThat(initialMovements).isNotEmpty();
        for (StockMovement m : initialMovements) {
            assertThat(m.getWarehouse()).isEqualTo(defaultWarehouse);
            assertThat(m.getLocation()).isEqualTo(defaultLocation);
        }

        // Verify commercial workflow seeding
        verify(clientRepository).saveAll(clientListCaptor.capture());
        List<Client> clients = clientListCaptor.getValue();
        assertThat(clients).hasSize(3);

        verify(purchaseOrderRepository).saveAll(purchaseOrderListCaptor.capture());
        List<PurchaseOrder> pos = purchaseOrderListCaptor.getValue();
        assertThat(pos).hasSize(2);
        assertThat(pos).anyMatch(po -> po.getStatus() == PurchaseOrderStatus.RECEIVED && po.getOrderNumber().equals("PO-2026-0001"));

        verify(saleOrderRepository).saveAll(saleOrderListCaptor.capture());
        List<SaleOrder> sos = saleOrderListCaptor.getValue();
        assertThat(sos).hasSize(2);
        assertThat(sos).anyMatch(so -> so.getStatus() == SaleOrderStatus.DELIVERED && so.getOrderNumber().equals("SO-2026-0010"));
        assertThat(sos).anyMatch(so -> so.getStatus() == SaleOrderStatus.DRAFT && so.getOrderNumber().equals("SO-2026-0020"));

        verify(customerInvoiceRepository).saveAll(customerInvoiceListCaptor.capture());
        List<CustomerInvoice> invoices = customerInvoiceListCaptor.getValue();
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getStatus()).isEqualTo(CustomerInvoiceStatus.ISSUED);
        assertThat(invoices.get(0).getInvoiceNumber()).isEqualTo("FAC-2026-0001");
        assertThat(invoices.get(0).getSaleOrder()).isNotNull();

        verify(paymentRepository).saveAll(paymentListCaptor.capture());
        List<Payment> payments = paymentListCaptor.getValue();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getPaymentType()).isEqualTo(PaymentType.CUSTOMER_PAYMENT);
        assertThat(payments.get(0).getCustomerInvoice()).isEqualTo(invoices.get(0));

        verify(stockTransferRepository).save(stockTransferCaptor.capture());
        StockTransfer transfer = stockTransferCaptor.getValue();
        assertThat(transfer.getStatus()).isEqualTo(StockTransferStatus.COMPLETED);
        assertThat(transfer.getTransferNumber()).isEqualTo("TRF-2026-0001");
        assertThat(transfer.getSourceWarehouse()).isEqualTo(defaultWarehouse);
        assertThat(transfer.getDestinationWarehouse()).isEqualTo(secondaryWarehouse);
        assertThat(transfer.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("If warehouse_stocks row already exists, seeder does not create duplicates")
    void testRun_WhenWarehouseStocksAlreadyExist_DoesNotDuplicate() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(unitRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(supplierRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(articleRepository.saveAll(any())).thenAnswer(inv -> {
            List<Article> articles = inv.getArgument(0);
            long id = 1L;
            for (Article a : articles) {
                a.setId(id++);
            }
            return articles;
        });

        when(warehouseRepository.findByCode("WH-MAIN")).thenReturn(Optional.of(defaultWarehouse));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(1L, "LOC-GEN")).thenReturn(Optional.of(defaultLocation));

        WarehouseStock existingStock = WarehouseStock.builder()
                .warehouse(defaultWarehouse)
                .location(defaultLocation)
                .quantity(new BigDecimal("150.0000"))
                .build();
        when(warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(any(), eq(1L), eq(10L)))
                .thenReturn(Optional.of(existingStock));

        when(purchaseOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerInvoiceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(warehouseRepository.findByCode("WH-NORTH")).thenReturn(Optional.of(secondaryWarehouse));
        when(warehouseLocationRepository.findByWarehouseIdAndCode(2L, "LOC-NORTH-01")).thenReturn(Optional.of(secondaryLocation));
        when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        verify(warehouseStockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("If default warehouse is not found, seeder gracefully skips warehouse_stocks and stock transfers")
    void testRun_WhenWarehouseNotFound_GracefullySkips() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(unitRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(supplierRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(articleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(warehouseRepository.findByCode("WH-MAIN")).thenReturn(Optional.empty());
        when(warehouseRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        when(purchaseOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleOrderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerInvoiceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        dataSeeder.run();

        verify(warehouseStockRepository, never()).saveAll(any());
        verify(stockMovementRepository).saveAll(any());
        verify(stockTransferRepository, never()).save(any());
    }
}
