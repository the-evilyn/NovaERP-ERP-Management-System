package com.novaerp.backend.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class WarehouseRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("Default warehouse WH-MAIN and location LOC-GEN are seeded by migration")
    void testDefaultWarehouseAndLocation_ExistAfterMigration() {
        Optional<Warehouse> defaultWhOpt = warehouseRepository.findByCode("WH-MAIN");
        assertThat(defaultWhOpt).isPresent();

        Warehouse defaultWh = defaultWhOpt.get();
        assertThat(defaultWh.getName()).contains("Entrepôt Principal");
        assertThat(defaultWh.isActive()).isTrue();
        assertThat(defaultWh.isDefault()).isTrue();

        Optional<WarehouseLocation> defaultLocOpt = warehouseLocationRepository.findByWarehouseIdAndCode(defaultWh.getId(), "LOC-GEN");
        assertThat(defaultLocOpt).isPresent();

        WarehouseLocation defaultLoc = defaultLocOpt.get();
        assertThat(defaultLoc.getName()).isEqualTo("Zone Générale");
        assertThat(defaultLoc.isActive()).isTrue();
        assertThat(defaultLoc.isDefault()).isTrue();
    }

    @Test
    @DisplayName("Migration preserves exact article stock quantities in warehouse_stocks")
    void testArticleStocksMigration_ExactMatchWithArticles() {
        long articleCount = articleRepository.count();
        long warehouseStockCount = warehouseStockRepository.count();

        assertThat(articleCount).isGreaterThan(0);
        assertThat(warehouseStockCount).isEqualTo(articleCount);

        BigDecimal articleTotalStock = articleRepository.sumTotalStockQuantity();
        BigDecimal warehouseTotalStock = warehouseStockRepository.sumTotalStockQuantity();

        assertThat(articleTotalStock).isNotNull();
        assertThat(warehouseTotalStock).isNotNull();
        assertThat(warehouseTotalStock.compareTo(articleTotalStock))
                .withFailMessage("Warehouse stock sum (%s) must match article stock sum (%s)", warehouseTotalStock, articleTotalStock)
                .isZero();
    }

    @Test
    @DisplayName("Historical stock movements are backfilled with default warehouse and location")
    void testHistoricalStockMovements_BackfilledWithDefaultWarehouseAndLocation() {
        long totalMovements = stockMovementRepository.count();
        assertThat(totalMovements).isGreaterThan(0);

        Warehouse defaultWh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        WarehouseLocation defaultLoc = warehouseLocationRepository.findByWarehouseIdAndCode(defaultWh.getId(), "LOC-GEN").orElseThrow();

        List<StockMovement> movements = stockMovementRepository.findAll();
        for (StockMovement m : movements) {
            assertThat(m.getWarehouse()).isNotNull();
            assertThat(m.getWarehouse().getId()).isEqualTo(defaultWh.getId());
            assertThat(m.getLocation()).isNotNull();
            assertThat(m.getLocation().getId()).isEqualTo(defaultLoc.getId());
        }
    }

    @Test
    @DisplayName("Can create new warehouse with multiple locations")
    void testCreateNewWarehouseAndLocations() {
        Warehouse wh = Warehouse.builder()
                .code("WH-TNG-TEST")
                .name("Entrepôt Tanger Test")
                .address("Zone Franche, Tanger")
                .active(true)
                .isDefault(false)
                .build();
        Warehouse savedWh = warehouseRepository.save(wh);
        assertThat(savedWh.getId()).isNotNull();

        WarehouseLocation locA1 = WarehouseLocation.builder()
                .warehouse(savedWh)
                .code("LOC-A1")
                .name("Allée A - Rack 1")
                .active(true)
                .build();
        WarehouseLocation locA2 = WarehouseLocation.builder()
                .warehouse(savedWh)
                .code("LOC-A2")
                .name("Allée A - Rack 2")
                .active(true)
                .build();

        warehouseLocationRepository.saveAll(List.of(locA1, locA2));

        List<WarehouseLocation> locations = warehouseLocationRepository.findByWarehouseId(savedWh.getId());
        assertThat(locations).hasSize(2);
        assertThat(locations).extracting(WarehouseLocation::getCode).containsExactlyInAnyOrder("LOC-A1", "LOC-A2");
    }

    @Test
    @DisplayName("Correction 1: Same article can have stock in multiple locations inside the same warehouse")
    void testMultiLocationStockForSameArticle() {
        Warehouse defaultWh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        Article sampleArticle = articleRepository.findAll().get(0);

        WarehouseLocation loc2 = WarehouseLocation.builder()
                .warehouse(defaultWh)
                .code("LOC-TEST-SHELF-B")
                .name("Rayon B - Étagère 2")
                .active(true)
                .build();
        WarehouseLocation savedLoc2 = warehouseLocationRepository.save(loc2);

        BigDecimal initialArticleWhStock = warehouseStockRepository.sumQuantityByArticleIdAndWarehouseId(sampleArticle.getId(), defaultWh.getId());

        // Add additional stock in location 2
        BigDecimal additionalQuantity = new BigDecimal("25.5000");
        WarehouseStock extraStock = WarehouseStock.builder()
                .article(sampleArticle)
                .warehouse(defaultWh)
                .location(savedLoc2)
                .quantity(additionalQuantity)
                .minQuantity(BigDecimal.ZERO)
                .build();
        warehouseStockRepository.save(extraStock);

        BigDecimal updatedArticleWhStock = warehouseStockRepository.sumQuantityByArticleIdAndWarehouseId(sampleArticle.getId(), defaultWh.getId());
        assertThat(updatedArticleWhStock).isEqualByComparingTo(initialArticleWhStock.add(additionalQuantity));

        List<WarehouseStock> articleStocks = warehouseStockRepository.findByArticleIdAndWarehouseId(sampleArticle.getId(), defaultWh.getId());
        assertThat(articleStocks).hasSize(2);
    }

    @Test
    @DisplayName("Unique constraint (article_id, warehouse_id, location_id) prevents duplicate entries for the exact same location")
    void testUniqueConstraint_RejectsDuplicateLocationStock() {
        Warehouse defaultWh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        WarehouseLocation defaultLoc = warehouseLocationRepository.findByWarehouseIdAndCode(defaultWh.getId(), "LOC-GEN").orElseThrow();
        Article sampleArticle = articleRepository.findAll().get(0);

        WarehouseStock duplicate = WarehouseStock.builder()
                .article(sampleArticle)
                .warehouse(defaultWh)
                .location(defaultLoc)
                .quantity(new BigDecimal("10.0000"))
                .build();

        assertThatThrownBy(() -> {
            warehouseStockRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
