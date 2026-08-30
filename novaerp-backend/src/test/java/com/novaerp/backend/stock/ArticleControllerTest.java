package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.ArticleRequest;
import com.novaerp.backend.stock.dto.ArticleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ArticleSupplierPriceRepository articleSupplierPriceRepository;

    @Mock
    private StockImportExportService importExportService;

    @InjectMocks
    private ArticleController articleController;

    private Article sampleArticle;
    private Category sampleCategory;
    private Unit sampleUnit;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder().id(1L).name("Electronics").build();
        sampleUnit = Unit.builder().id(1L).name("Piece").symbol("pc").build();

        sampleArticle = Article.builder()
                .id(1L)
                .reference("ART-001")
                .designation("Wireless Mouse")
                .brand("Logitech")
                .category(sampleCategory)
                .unit(sampleUnit)
                .purchasePriceHt(new BigDecimal("50.00"))
                .unitCostTtc(new BigDecimal("60.00"))
                .salePriceHt(new BigDecimal("80.00"))
                .stockQuantity(new BigDecimal("100"))
                .minStockQuantity(new BigDecimal("10"))
                .build();
    }

    @Test
    @DisplayName("list returns paginated articles")
    void testListArticles() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Article> page = new PageImpl<>(List.of(sampleArticle), pageable, 1);
        when(articleRepository.findAll(pageable)).thenReturn(page);

        Page<ArticleResponse> result = articleController.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reference()).isEqualTo("ART-001");
    }

    @Test
    @DisplayName("get returns article when found")
    void testGetArticle_Success() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));

        ArticleResponse response = articleController.get(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.reference()).isEqualTo("ART-001");
    }

    @Test
    @DisplayName("get throws NOT_FOUND when article does not exist")
    void testGetArticle_NotFound() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleController.get(999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("create saves article and returns 201 CREATED")
    void testCreateArticle_Success() {
        when(articleRepository.existsByReference("NEW-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(sampleUnit));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            a.setId(2L);
            return a;
        });

        ArticleRequest request = new ArticleRequest(
                "NEW-001", "New Device", "BrandX", "123456",
                1L, 1L,
                new BigDecimal("10.00"), new BigDecimal("12.00"), new BigDecimal("20.00"),
                new BigDecimal("5"), false, "Description", "Notes"
        );

        ResponseEntity<ArticleResponse> response = articleController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().reference()).isEqualTo("NEW-001");
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    @DisplayName("create with duplicate reference throws CONFLICT")
    void testCreateArticle_DuplicateReference() {
        when(articleRepository.existsByReference("ART-001")).thenReturn(true);

        ArticleRequest request = new ArticleRequest(
                "ART-001", "Duplicate Device", null, null,
                null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, false, null, null
        );

        assertThatThrownBy(() -> articleController.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    @DisplayName("delete removes article and returns 204 NO_CONTENT")
    void testDeleteArticle_Success() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(sampleArticle));

        ResponseEntity<Void> response = articleController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(articleRepository).delete(sampleArticle);
    }
}
