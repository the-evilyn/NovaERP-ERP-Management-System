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
    @DisplayName("exportArticles() returns CSV headers")
    void testExportArticles() {
        when(articleRepository.findAll()).thenReturn(List.of());

        byte[] bytes = service.exportArticles();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("reference,designation,brand,barcode,category,unit");
    }
}
