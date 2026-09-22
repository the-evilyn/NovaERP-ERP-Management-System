package com.novaerp.backend.stock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StockTransferRepositoryTest {

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private StockTransferItemRepository stockTransferItemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @BeforeEach
    void setUp() {
        Warehouse defaultWh = warehouseRepository.findByCode("WH-MAIN").orElseGet(() ->
                warehouseRepository.save(Warehouse.builder()
                        .code("WH-MAIN")
                        .name("Entrepôt Principal (Siège)")
                        .description("Site logistique central et stockage principal")
                        .address("Casablanca, Maroc")
                        .active(true)
                        .isDefault(true)
                        .build()));

        WarehouseLocation defaultLoc = warehouseLocationRepository.findByWarehouseIdAndCode(defaultWh.getId(), "LOC-GEN").orElseGet(() ->
                warehouseLocationRepository.save(WarehouseLocation.builder()
                        .warehouse(defaultWh)
                        .code("LOC-GEN")
                        .name("Zone Générale")
                        .description("Emplacement général de stockage par défaut")
                        .active(true)
                        .isDefault(true)
                        .build()));

        if (articleRepository.count() < 2) {
            Category cat = categoryRepository.save(Category.builder().name("Test Cat TRF " + System.nanoTime()).description("Test").build());
            Unit unit = unitRepository.save(Unit.builder().name("Piece " + System.nanoTime()).symbol("PCS" + (System.nanoTime() % 10000)).build());
            Article a1 = articleRepository.save(Article.builder()
                    .reference("ART-TRF-1-" + System.nanoTime())
                    .designation("Article TRF Test 1")
                    .category(cat)
                    .unit(unit)
                    .purchasePriceHt(new BigDecimal("100.0000"))
                    .unitCostTtc(new BigDecimal("120.0000"))
                    .salePriceHt(new BigDecimal("150.0000"))
                    .stockQuantity(new BigDecimal("50.0000"))
                    .minStockQuantity(new BigDecimal("10.0000"))
                    .build());
            Article a2 = articleRepository.save(Article.builder()
                    .reference("ART-TRF-2-" + System.nanoTime())
                    .designation("Article TRF Test 2")
                    .category(cat)
                    .unit(unit)
                    .purchasePriceHt(new BigDecimal("50.0000"))
                    .unitCostTtc(new BigDecimal("60.0000"))
                    .salePriceHt(new BigDecimal("80.0000"))
                    .stockQuantity(new BigDecimal("20.0000"))
                    .minStockQuantity(new BigDecimal("5.0000"))
                    .build());

            warehouseStockRepository.save(WarehouseStock.builder()
                    .article(a1)
                    .warehouse(defaultWh)
                    .location(defaultLoc)
                    .quantity(a1.getStockQuantity())
                    .minQuantity(a1.getMinStockQuantity())
                    .build());
            warehouseStockRepository.save(WarehouseStock.builder()
                    .article(a2)
                    .warehouse(defaultWh)
                    .location(defaultLoc)
                    .quantity(a2.getStockQuantity())
                    .minQuantity(a2.getMinStockQuantity())
                    .build());
        }
    }

    @Test
    @DisplayName("Can save stock transfer with items and retrieve by transfer number")
    void testSaveStockTransferWithItems() {
        Warehouse srcWh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        WarehouseLocation srcLoc = warehouseLocationRepository.findByWarehouseIdAndCode(srcWh.getId(), "LOC-GEN").orElseThrow();

        Warehouse dstWh = warehouseRepository.save(Warehouse.builder()
                .code("WH-DST-TEST")
                .name("Entrepôt Destination Test")
                .active(true)
                .build());

        WarehouseLocation dstLoc = warehouseLocationRepository.save(WarehouseLocation.builder()
                .warehouse(dstWh)
                .code("LOC-DST-1")
                .name("Zone Réception")
                .active(true)
                .build());

        List<Article> articles = articleRepository.findAll();
        assertThat(articles).isNotEmpty();
        Article article1 = articles.get(0);

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber("TRF-2026-0001")
                .sourceWarehouse(srcWh)
                .sourceLocation(srcLoc)
                .destinationWarehouse(dstWh)
                .destinationLocation(dstLoc)
                .notes("Transfert de test")
                .build();

        StockTransferItem item1 = StockTransferItem.builder()
                .article(article1)
                .quantity(new BigDecimal("10.0000"))
                .build();

        transfer.addItem(item1);

        StockTransfer saved = stockTransferRepository.save(transfer);
        stockTransferRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTransferNumber()).isEqualTo("TRF-2026-0001");
        assertThat(saved.getStatus()).isEqualTo(StockTransferStatus.DRAFT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getId()).isNotNull();

        // Verify findByTransferNumber
        Optional<StockTransfer> foundOpt = stockTransferRepository.findByTransferNumber("TRF-2026-0001");
        assertThat(foundOpt).isPresent();
        assertThat(foundOpt.get().getSourceWarehouse().getCode()).isEqualTo("WH-MAIN");
        assertThat(foundOpt.get().getDestinationWarehouse().getCode()).isEqualTo("WH-DST-TEST");

        // Verify existsByTransferNumber
        assertThat(stockTransferRepository.existsByTransferNumber("TRF-2026-0001")).isTrue();
        assertThat(stockTransferRepository.existsByTransferNumber("TRF-NON-EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Find with details eagerly fetches all associations")
    void testFindWithDetailsById() {
        Warehouse srcWh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        WarehouseLocation srcLoc = warehouseLocationRepository.findByWarehouseIdAndCode(srcWh.getId(), "LOC-GEN").orElseThrow();

        Warehouse dstWh = warehouseRepository.save(Warehouse.builder()
                .code("WH-DST-DETAILS")
                .name("Entrepôt Destination Details")
                .active(true)
                .build());

        Article article = articleRepository.findAll().get(0);

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber("TRF-2026-0002")
                .sourceWarehouse(srcWh)
                .sourceLocation(srcLoc)
                .destinationWarehouse(dstWh)
                .build();

        transfer.addItem(StockTransferItem.builder()
                .article(article)
                .quantity(new BigDecimal("5.0000"))
                .build());

        StockTransfer saved = stockTransferRepository.saveAndFlush(transfer);

        Optional<StockTransfer> detailedOpt = stockTransferRepository.findWithDetailsById(saved.getId());
        assertThat(detailedOpt).isPresent();
        StockTransfer detailed = detailedOpt.get();
        assertThat(detailed.getSourceWarehouse()).isNotNull();
        assertThat(detailed.getSourceWarehouse().getCode()).isEqualTo("WH-MAIN");
        assertThat(detailed.getSourceLocation()).isNotNull();
        assertThat(detailed.getSourceLocation().getCode()).isEqualTo("LOC-GEN");
        assertThat(detailed.getDestinationWarehouse()).isNotNull();
        assertThat(detailed.getDestinationWarehouse().getCode()).isEqualTo("WH-DST-DETAILS");
        assertThat(detailed.getItems()).hasSize(1);
        assertThat(detailed.getItems().get(0).getArticle().getReference()).isEqualTo(article.getReference());
    }

    @Test
    @DisplayName("Pessimistic lock query findByIdForUpdate locks transfer row")
    void testFindByIdForUpdate() {
        Warehouse wh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber("TRF-2026-0003")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .build();

        StockTransfer saved = stockTransferRepository.saveAndFlush(transfer);

        Optional<StockTransfer> locked = stockTransferRepository.findByIdForUpdate(saved.getId());
        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("Pessimistic lock query findAllByIdInForUpdate locks articles in ascending order")
    void testFindAllByIdInForUpdate() {
        List<Article> articles = articleRepository.findAll();
        assertThat(articles.size()).isGreaterThanOrEqualTo(2);

        List<Long> ids = List.of(articles.get(1).getId(), articles.get(0).getId());
        List<Article> lockedArticles = articleRepository.findAllByIdInForUpdate(ids);

        assertThat(lockedArticles).hasSize(2);
        assertThat(lockedArticles.get(0).getId()).isLessThan(lockedArticles.get(1).getId());
    }

    @Test
    @DisplayName("Pessimistic lock query findByArticleIdAndWarehouseIdAndLocationIdForUpdate locks warehouse stock row")
    void testFindWarehouseStockForUpdate() {
        Warehouse wh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        WarehouseLocation loc = warehouseLocationRepository.findByWarehouseIdAndCode(wh.getId(), "LOC-GEN").orElseThrow();
        Article article = articleRepository.findAll().get(0);

        Optional<WarehouseStock> stockOpt = warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationIdForUpdate(
                article.getId(), wh.getId(), loc.getId()
        );

        assertThat(stockOpt).isPresent();
        assertThat(stockOpt.get().getQuantity()).isNotNull();
    }

    @Test
    @DisplayName("Filtering transfers by status and warehouse pagination")
    void testQueryFilteringAndPagination() {
        Warehouse wh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();

        StockTransfer draft = StockTransfer.builder()
                .transferNumber("TRF-2026-0004")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .status(StockTransferStatus.DRAFT)
                .build();

        StockTransfer completed = StockTransfer.builder()
                .transferNumber("TRF-2026-0005")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .status(StockTransferStatus.COMPLETED)
                .build();

        stockTransferRepository.saveAllAndFlush(List.of(draft, completed));

        Page<StockTransfer> drafts = stockTransferRepository.findByStatus(StockTransferStatus.DRAFT, PageRequest.of(0, 10));
        assertThat(drafts.getContent()).extracting(StockTransfer::getTransferNumber).contains("TRF-2026-0004");
        assertThat(drafts.getContent()).extracting(StockTransfer::getTransferNumber).doesNotContain("TRF-2026-0005");

        Page<StockTransfer> byWarehouse = stockTransferRepository.findByWarehouseId(wh.getId(), PageRequest.of(0, 10));
        assertThat(byWarehouse.getContent()).extracting(StockTransfer::getTransferNumber)
                .contains("TRF-2026-0004", "TRF-2026-0005");

        assertThat(stockTransferRepository.countByStatus(StockTransferStatus.DRAFT)).isGreaterThanOrEqualTo(1);
        assertThat(stockTransferRepository.countTotalTransfers()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Unique constraint on transfer_number rejects duplicates")
    void testDuplicateTransferNumberRejected() {
        Warehouse wh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();

        StockTransfer t1 = StockTransfer.builder()
                .transferNumber("TRF-DUPLICATE")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .build();
        stockTransferRepository.saveAndFlush(t1);

        StockTransfer t2 = StockTransfer.builder()
                .transferNumber("TRF-DUPLICATE")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .build();

        assertThatThrownBy(() -> stockTransferRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Cascade delete removes transfer items when transfer is deleted")
    void testCascadeDeleteTransferItems() {
        Warehouse wh = warehouseRepository.findByCode("WH-MAIN").orElseThrow();
        Article article = articleRepository.findAll().get(0);

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber("TRF-CASCADE-TEST")
                .sourceWarehouse(wh)
                .destinationWarehouse(wh)
                .build();

        transfer.addItem(StockTransferItem.builder()
                .article(article)
                .quantity(new BigDecimal("15.0000"))
                .build());

        StockTransfer saved = stockTransferRepository.saveAndFlush(transfer);
        Long transferId = saved.getId();

        List<StockTransferItem> itemsBefore = stockTransferItemRepository.findByTransferId(transferId);
        assertThat(itemsBefore).hasSize(1);

        stockTransferRepository.delete(saved);
        stockTransferRepository.flush();

        List<StockTransferItem> itemsAfter = stockTransferItemRepository.findByTransferId(transferId);
        assertThat(itemsAfter).isEmpty();
    }
}
