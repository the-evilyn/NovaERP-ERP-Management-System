package com.novaerp.backend.config;

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
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Captor
    private ArgumentCaptor<List<WarehouseStock>> warehouseStockListCaptor;

    @Captor
    private ArgumentCaptor<List<StockMovement>> stockMovementListCaptor;

    private Warehouse defaultWarehouse;
    private WarehouseLocation defaultLocation;

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

        verify(stockMovementRepository).saveAll(stockMovementListCaptor.capture());
        List<StockMovement> savedMovements = stockMovementListCaptor.getValue();
        assertThat(savedMovements).isNotEmpty();
        for (StockMovement m : savedMovements) {
            assertThat(m.getWarehouse()).isEqualTo(defaultWarehouse);
            assertThat(m.getLocation()).isEqualTo(defaultLocation);
        }
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

        dataSeeder.run();

        verify(warehouseStockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("If default warehouse is not found, seeder gracefully skips warehouse_stocks")
    void testRun_WhenWarehouseNotFound_GracefullySkips() {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        when(userRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(unitRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(supplierRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(articleRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        when(warehouseRepository.findByCode("WH-MAIN")).thenReturn(Optional.empty());
        when(warehouseRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

        dataSeeder.run();

        verify(warehouseStockRepository, never()).saveAll(any());
        verify(stockMovementRepository).saveAll(any());
    }
}
