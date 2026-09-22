package com.novaerp.backend.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaerp.backend.stock.dto.StockTransferItemRequest;
import com.novaerp.backend.stock.dto.StockTransferRequest;
import com.novaerp.backend.stock.dto.StockTransferResponse;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockTransferService stockTransferService;

    @Autowired
    private StockTransferRepository stockTransferRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private WarehouseLocationRepository warehouseLocationRepository;

    @Autowired
    private WarehouseStockRepository warehouseStockRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private Warehouse srcWarehouse;
    private WarehouseLocation srcLocation;
    private Warehouse dstWarehouse;
    private WarehouseLocation dstLocation;
    private Article article1;
    private Article article2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        srcWarehouse = warehouseRepository.findByCode("WH-MAIN").orElseGet(() ->
                warehouseRepository.save(Warehouse.builder()
                        .code("WH-MAIN")
                        .name("Entrepôt Principal (Siège)")
                        .description("Site logistique central et stockage principal")
                        .address("Casablanca, Maroc")
                        .active(true)
                        .isDefault(true)
                        .build()));

        srcLocation = warehouseLocationRepository.findByWarehouseIdAndCode(srcWarehouse.getId(), "LOC-GEN").orElseGet(() ->
                warehouseLocationRepository.save(WarehouseLocation.builder()
                        .warehouse(srcWarehouse)
                        .code("LOC-GEN")
                        .name("Zone Générale")
                        .description("Emplacement général de stockage par défaut")
                        .active(true)
                        .isDefault(true)
                        .build()));

        dstWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-DST-INT")
                .name("Entrepôt Destination Integration")
                .active(true)
                .build());

        dstLocation = warehouseLocationRepository.save(WarehouseLocation.builder()
                .warehouse(dstWarehouse)
                .code("LOC-DST-INT")
                .name("Zone Réception Int")
                .active(true)
                .build());

        if (articleRepository.count() < 2) {
            Category cat = categoryRepository.save(Category.builder().name("Test Cat TRF INT " + System.nanoTime()).description("Test").build());
            Unit unit = unitRepository.save(Unit.builder().name("Piece " + System.nanoTime()).symbol("PCS" + (System.nanoTime() % 10000)).build());
            Article a1 = articleRepository.save(Article.builder()
                    .reference("ART-TRF-INT-1-" + System.nanoTime())
                    .designation("Article TRF INT Test 1")
                    .category(cat)
                    .unit(unit)
                    .purchasePriceHt(new BigDecimal("100.0000"))
                    .unitCostTtc(new BigDecimal("120.0000"))
                    .salePriceHt(new BigDecimal("150.0000"))
                    .stockQuantity(new BigDecimal("50.0000"))
                    .minStockQuantity(new BigDecimal("10.0000"))
                    .build());
            Article a2 = articleRepository.save(Article.builder()
                    .reference("ART-TRF-INT-2-" + System.nanoTime())
                    .designation("Article TRF INT Test 2")
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
                    .warehouse(srcWarehouse)
                    .location(srcLocation)
                    .quantity(a1.getStockQuantity())
                    .minQuantity(a1.getMinStockQuantity())
                    .build());
            warehouseStockRepository.save(WarehouseStock.builder()
                    .article(a2)
                    .warehouse(srcWarehouse)
                    .location(srcLocation)
                    .quantity(a2.getStockQuantity())
                    .minQuantity(a2.getMinStockQuantity())
                    .build());
        }

        List<Article> articles = articleRepository.findAll();
        assertThat(articles.size()).isGreaterThanOrEqualTo(2);
        article1 = articles.get(0);
        article2 = articles.get(1);

        if (warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId()).isEmpty()) {
            warehouseStockRepository.save(WarehouseStock.builder()
                    .article(article1)
                    .warehouse(srcWarehouse)
                    .location(srcLocation)
                    .quantity(new BigDecimal("100.0000"))
                    .minQuantity(new BigDecimal("10.0000"))
                    .build());
        }
        if (warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(article2.getId(), srcWarehouse.getId(), srcLocation.getId()).isEmpty()) {
            warehouseStockRepository.save(WarehouseStock.builder()
                    .article(article2)
                    .warehouse(srcWarehouse)
                    .location(srcLocation)
                    .quantity(new BigDecimal("100.0000"))
                    .minQuantity(new BigDecimal("10.0000"))
                    .build());
        }

        adminUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .findFirst()
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin-transfers@novaerp.local")
                        .password("password")
                        .fullName("Admin Transfers")
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build()));
    }

    @Test
    @DisplayName("End-to-End: Create draft transfer, complete it, verify stocks, movements, and invariant Article.stockQuantity")
    void testCompleteTransfer_EndToEnd() {
        WarehouseStock srcStockBefore = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();

        BigDecimal initialSrcStock = srcStockBefore.getQuantity();
        BigDecimal initialArticleStock = article1.getStockQuantity();
        BigDecimal transferQty = new BigDecimal("5.0000");

        // Ensure sufficient stock in source
        if (initialSrcStock.compareTo(transferQty) < 0) {
            srcStockBefore.setQuantity(initialSrcStock.add(new BigDecimal("50.0000")));
            warehouseStockRepository.saveAndFlush(srcStockBefore);
            initialSrcStock = srcStockBefore.getQuantity();
        }

        BigDecimal initialDstStock = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), dstWarehouse.getId(), dstLocation.getId())
                .map(WarehouseStock::getQuantity)
                .orElse(BigDecimal.ZERO);

        long movementsBefore = stockMovementRepository.count();

        // 1. Create draft transfer
        StockTransferRequest createRequest = new StockTransferRequest(
                srcWarehouse.getId(),
                srcLocation.getId(),
                dstWarehouse.getId(),
                dstLocation.getId(),
                "Integration test transfer",
                List.of(new StockTransferItemRequest(article1.getId(), transferQty))
        );

        StockTransferResponse draft = stockTransferService.create(createRequest, adminUser);
        assertThat(draft.status()).isEqualTo(StockTransferStatus.DRAFT);
        assertThat(draft.transferNumber()).startsWith("TRF-");
        assertThat(draft.items()).hasSize(1);

        // 2. Complete the transfer
        StockTransferResponse completed = stockTransferService.complete(draft.id(), adminUser);
        assertThat(completed.status()).isEqualTo(StockTransferStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();

        // 3. Verify source warehouse stock decreased
        WarehouseStock srcStockAfter = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();
        assertThat(srcStockAfter.getQuantity()).isEqualByComparingTo(initialSrcStock.subtract(transferQty));

        // 4. Verify destination warehouse stock increased
        WarehouseStock dstStockAfter = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), dstWarehouse.getId(), dstLocation.getId())
                .orElseThrow();
        assertThat(dstStockAfter.getQuantity()).isEqualByComparingTo(initialDstStock.add(transferQty));

        // 5. CRITICAL: Article total company-wide stockQuantity remains EXACTLY unchanged
        Article articleAfter = articleRepository.findById(article1.getId()).orElseThrow();
        assertThat(articleAfter.getStockQuantity()).isEqualByComparingTo(initialArticleStock);

        // 6. Verify exactly two movements (one OUT, one IN) were recorded
        long movementsAfter = stockMovementRepository.count();
        assertThat(movementsAfter).isEqualTo(movementsBefore + 2);

        List<StockMovement> transferMovements = stockMovementRepository.findAll().stream()
                .filter(m -> draft.transferNumber().equals(m.getReference()))
                .toList();
        assertThat(transferMovements).hasSize(2);

        StockMovement outMv = transferMovements.stream().filter(m -> m.getType() == StockMovementType.OUT).findFirst().orElseThrow();
        assertThat(outMv.getWarehouse().getId()).isEqualTo(srcWarehouse.getId());
        assertThat(outMv.getLocation().getId()).isEqualTo(srcLocation.getId());
        assertThat(outMv.getQuantity()).isEqualByComparingTo(transferQty);

        StockMovement inMv = transferMovements.stream().filter(m -> m.getType() == StockMovementType.IN).findFirst().orElseThrow();
        assertThat(inMv.getWarehouse().getId()).isEqualTo(dstWarehouse.getId());
        assertThat(inMv.getLocation().getId()).isEqualTo(dstLocation.getId());
        assertThat(inMv.getQuantity()).isEqualByComparingTo(transferQty);
    }

    @Test
    @DisplayName("Double completion rejection: cannot complete an already COMPLETED transfer")
    void testDoubleCompletion_Rejection() {
        WarehouseStock srcStock = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();
        srcStock.setQuantity(new BigDecimal("100.0000"));
        warehouseStockRepository.saveAndFlush(srcStock);

        StockTransferRequest createRequest = new StockTransferRequest(
                srcWarehouse.getId(),
                srcLocation.getId(),
                dstWarehouse.getId(),
                dstLocation.getId(),
                null,
                List.of(new StockTransferItemRequest(article1.getId(), new BigDecimal("1.0000")))
        );

        StockTransferResponse draft = stockTransferService.create(createRequest, adminUser);
        stockTransferService.complete(draft.id(), adminUser);

        // Second complete call MUST fail with BAD_REQUEST
        assertThatThrownBy(() -> stockTransferService.complete(draft.id(), adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only DRAFT transfers can be completed");
    }

    @Test
    @DisplayName("Cancel draft transfer: status becomes CANCELLED without modifying stocks or movements")
    void testCancelDraftTransfer_EndToEnd() {
        long movementsBefore = stockMovementRepository.count();

        StockTransferRequest createRequest = new StockTransferRequest(
                srcWarehouse.getId(),
                srcLocation.getId(),
                dstWarehouse.getId(),
                dstLocation.getId(),
                null,
                List.of(new StockTransferItemRequest(article1.getId(), new BigDecimal("2.0000")))
        );

        StockTransferResponse draft = stockTransferService.create(createRequest, adminUser);
        assertThat(draft.status()).isEqualTo(StockTransferStatus.DRAFT);

        StockTransferResponse cancelled = stockTransferService.cancel(draft.id(), adminUser);
        assertThat(cancelled.status()).isEqualTo(StockTransferStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();

        assertThat(stockMovementRepository.count()).isEqualTo(movementsBefore);
    }

    @Test
    @DisplayName("Rollback on failure: multi-item transfer where second item has insufficient stock rolls back all mutations")
    void testRollbackOnFailure_MultiItem() {
        WarehouseStock srcStock1 = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();
        srcStock1.setQuantity(new BigDecimal("100.0000"));
        warehouseStockRepository.saveAndFlush(srcStock1);

        WarehouseStock srcStock2 = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article2.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElse(null);
        if (srcStock2 != null) {
            srcStock2.setQuantity(new BigDecimal("1.0000")); // only 1 available, we will request 100
            warehouseStockRepository.saveAndFlush(srcStock2);
        }

        BigDecimal srcStock1Before = srcStock1.getQuantity();
        long movementsBefore = stockMovementRepository.count();

        StockTransferRequest createRequest = new StockTransferRequest(
                srcWarehouse.getId(),
                srcLocation.getId(),
                dstWarehouse.getId(),
                dstLocation.getId(),
                "Multi-item rollback test",
                List.of(
                        new StockTransferItemRequest(article1.getId(), new BigDecimal("10.0000")),
                        new StockTransferItemRequest(article2.getId(), new BigDecimal("100.0000"))
                )
        );

        StockTransferResponse draft = stockTransferService.create(createRequest, adminUser);

        // Completion MUST fail on item 2
        assertThatThrownBy(() -> stockTransferService.complete(draft.id(), adminUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient stock");

        // Verify Article 1 source stock was NOT decremented
        WarehouseStock srcStock1After = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();
        assertThat(srcStock1After.getQuantity()).isEqualByComparingTo(srcStock1Before);

        // Verify transfer remains DRAFT
        StockTransfer transferInDb = stockTransferRepository.findById(draft.id()).orElseThrow();
        assertThat(transferInDb.getStatus()).isEqualTo(StockTransferStatus.DRAFT);

        // Verify no movements were persisted
        assertThat(stockMovementRepository.count()).isEqualTo(movementsBefore);
    }

    @Test
    @DisplayName("Transfer number uniqueness across consecutive creations")
    void testTransferNumberUniqueness() {
        StockTransferRequest req1 = new StockTransferRequest(
                srcWarehouse.getId(), srcLocation.getId(), dstWarehouse.getId(), dstLocation.getId(),
                null, List.of(new StockTransferItemRequest(article1.getId(), new BigDecimal("1.0000")))
        );
        StockTransferRequest req2 = new StockTransferRequest(
                srcWarehouse.getId(), srcLocation.getId(), dstWarehouse.getId(), dstLocation.getId(),
                null, List.of(new StockTransferItemRequest(article1.getId(), new BigDecimal("1.0000")))
        );

        StockTransferResponse r1 = stockTransferService.create(req1, adminUser);
        StockTransferResponse r2 = stockTransferService.create(req2, adminUser);

        assertThat(r1.transferNumber()).isNotEqualTo(r2.transferNumber());
        assertThat(stockTransferRepository.existsByTransferNumber(r1.transferNumber())).isTrue();
        assertThat(stockTransferRepository.existsByTransferNumber(r2.transferNumber())).isTrue();
    }

    @Test
    @DisplayName("REST API: Create, get by ID, complete via HTTP endpoints with ADMIN user")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testRestEndpoints_CreateGetComplete() throws Exception {
        WarehouseStock srcStock = warehouseStockRepository
                .findByArticleIdAndWarehouseIdAndLocationId(article1.getId(), srcWarehouse.getId(), srcLocation.getId())
                .orElseThrow();
        srcStock.setQuantity(new BigDecimal("50.0000"));
        warehouseStockRepository.saveAndFlush(srcStock);

        StockTransferRequest request = new StockTransferRequest(
                srcWarehouse.getId(),
                srcLocation.getId(),
                dstWarehouse.getId(),
                dstLocation.getId(),
                "REST API test",
                List.of(new StockTransferItemRequest(article1.getId(), new BigDecimal("3.0000")))
        );

        // POST /api/stock/transfers -> 201 CREATED
        String createJson = mockMvc.perform(post("/api/stock/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.transferNumber").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        StockTransferResponse created = objectMapper.readValue(createJson, StockTransferResponse.class);

        // GET /api/stock/transfers/{id} -> 200 OK
        mockMvc.perform(get("/api/stock/transfers/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.transferNumber").value(created.transferNumber()));

        // POST /api/stock/transfers/{id}/complete -> 200 OK
        mockMvc.perform(post("/api/stock/transfers/" + created.id() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }
}
