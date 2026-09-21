package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.ImportResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockImportExportServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleSupplierPriceRepository articleSupplierPriceRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockImportExportService service;

    @Test
    @DisplayName("exportCategories() formats CSV with header and data")
    void testExportCategories() {
        Category cat = Category.builder()
                .id(1L)
                .name("Hydraulique")
                .description("Vérins et flexibles")
                .createdAt(Instant.now())
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        byte[] bytes = service.exportCategories();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(com.novaerp.backend.common.csv.CsvUtils.UTF8_BOM);
        assertThat(csv).contains("name,description");
        assertThat(csv).contains("Hydraulique,Vérins et flexibles");
    }

    @Test
    @DisplayName("importCategories() creates new and skips existing")
    void testImportCategories() throws IOException {
        String csv = "name,description\n" +
                "Pneumatique,Composants air comprimé\n" +
                "Hydraulique,Existant\n" +
                ",Nom manquant\n";
        MockMultipartFile file = new MockMultipartFile("file", "cat.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(categoryRepository.existsByName("Pneumatique")).thenReturn(false);
        when(categoryRepository.existsByName("Hydraulique")).thenReturn(true);

        ImportResultResponse result = service.importCategories(file);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("exportSuppliers() formats CSV with suppliers data")
    void testExportSuppliers() {
        Supplier sup = Supplier.builder()
                .id(1L)
                .name("SNTL Logistique")
                .email("contact@sntl.ma")
                .phone("+212 522 000000")
                .address("Casablanca")
                .createdAt(Instant.now())
                .build();
        when(supplierRepository.findAll()).thenReturn(List.of(sup));

        byte[] bytes = service.exportSuppliers();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(com.novaerp.backend.common.csv.CsvUtils.UTF8_BOM);
        assertThat(csv).contains("name,email,phone,address");
        assertThat(csv).contains("SNTL Logistique");
        assertThat(csv).contains("contact@sntl.ma");
    }

    @Test
    @DisplayName("importSuppliers() creates new and skips existing")
    void testImportSuppliers() throws IOException {
        String csv = "name,email,phone,address\n" +
                "Fournisseur A,a@erp.ma,+2126000000,Tanger\n" +
                "Fournisseur B,b@erp.ma,+2126111111,Kenitra\n" +
                ",c@erp.ma,+2126222222,Fes\n";
        MockMultipartFile file = new MockMultipartFile("file", "sup.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(supplierRepository.existsByName("Fournisseur A")).thenReturn(false);
        when(supplierRepository.existsByName("Fournisseur B")).thenReturn(true);

        ImportResultResponse result = service.importSuppliers(file);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(supplierRepository, times(1)).save(any(Supplier.class));
    }

    @Test
    @DisplayName("exportArticles() returns CSV headers and UTF-8 BOM")
    void testExportArticles_Empty() {
        when(articleRepository.findAll()).thenReturn(List.of());

        byte[] bytes = service.exportArticles();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(com.novaerp.backend.common.csv.CsvUtils.UTF8_BOM);
        assertThat(csv).contains("reference,designation,brand,barcode,category,unit");
    }

    @Test
    @DisplayName("exportArticles() with filters calls searchArticlesList")
    void testExportArticles_Filtered() {
        Article art = Article.builder()
                .id(10L)
                .reference("REF-ELEC-01")
                .designation("Câble d'alimentation")
                .brand("Schneider")
                .stockQuantity(new java.math.BigDecimal("5.0000"))
                .minStockQuantity(new java.math.BigDecimal("10.0000"))
                .build();

        when(articleRepository.searchArticlesList("câble", 1L, true)).thenReturn(List.of(art));
        when(articleSupplierPriceRepository.findByArticleId(10L)).thenReturn(List.of());

        byte[] bytes = service.exportArticles("câble", 1L, true);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("REF-ELEC-01");
        assertThat(csv).contains("Câble d'alimentation");
        assertThat(csv).contains("Schneider");
        verify(articleRepository).searchArticlesList("câble", 1L, true);
    }

    @Test
    @DisplayName("exportStockMovements() formats stock movements with all headers and BOM")
    void testExportStockMovements() {
        Article art = Article.builder().id(1L).reference("ART-100").designation("Moteur AC").build();
        com.novaerp.backend.user.User user = com.novaerp.backend.user.User.builder()
                .id(1L).email("salma@novaerp.ma").fullName("Salma").build();
        Warehouse wh = Warehouse.builder().id(1L).name("Entrepôt Central").build();
        WarehouseLocation loc = WarehouseLocation.builder().id(1L).name("Allée A").build();

        StockMovement mv = StockMovement.builder()
                .id(50L)
                .article(art)
                .type(StockMovementType.IN)
                .quantity(new java.math.BigDecimal("25.5000"))
                .reference("PO-2026-001")
                .warehouse(wh)
                .location(loc)
                .createdBy(user)
                .note("Réception commande")
                .createdAt(Instant.parse("2026-09-21T10:15:30Z"))
                .build();

        when(stockMovementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(mv));

        byte[] bytes = service.exportStockMovements(null);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith(com.novaerp.backend.common.csv.CsvUtils.UTF8_BOM);
        assertThat(csv).contains("id,date,articleReference,articleDesignation,type,quantity,reference,warehouse,location,createdBy,notes");
        assertThat(csv).contains("50,2026-09-21T10:15:30Z,ART-100,Moteur AC,IN,25.5000,PO-2026-001,Entrepôt Central,Allée A,salma@novaerp.ma,Réception commande");
    }

    @Test
    @DisplayName("exportStockMovements() with articleId calls findByArticleIdOrderByCreatedAtDesc")
    void testExportStockMovements_WithArticleId() {
        when(stockMovementRepository.findByArticleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        byte[] bytes = service.exportStockMovements(1L);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("id,date,articleReference,articleDesignation,type,quantity,reference,warehouse,location,createdBy,notes");
        verify(stockMovementRepository).findByArticleIdOrderByCreatedAtDesc(1L);
    }
}
