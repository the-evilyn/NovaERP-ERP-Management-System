package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.CategoryRequest;
import com.novaerp.backend.stock.dto.CategoryResponse;
import com.novaerp.backend.stock.dto.ImportResultResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockImportExportService importExportService;

    @InjectMocks
    private CategoryController categoryController;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
                .id(1L)
                .name("Roulements")
                .description("Roulements à billes et à rouleaux")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("list() returns paged category responses")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(sampleCategory), pageable, 1);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        Page<CategoryResponse> result = categoryController.list(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Roulements");
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    @DisplayName("get(id) returns category when found")
    void testGet_Found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

        CategoryResponse result = categoryController.get(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Roulements");
    }

    @Test
    @DisplayName("get(id) throws NOT_FOUND when category missing")
    void testGet_NotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryController.get(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create() saves and returns 201 when name is unique")
    void testCreate_Success() {
        CategoryRequest req = new CategoryRequest("Outillage", "Outils d'atelier");
        when(categoryRepository.existsByName("Outillage")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ResponseEntity<CategoryResponse> res = categoryController.create(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().name()).isEqualTo("Outillage");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create() throws CONFLICT when name already exists")
    void testCreate_Conflict() {
        CategoryRequest req = new CategoryRequest("Roulements", "Duplicata");
        when(categoryRepository.existsByName("Roulements")).thenReturn(true);

        assertThatThrownBy(() -> categoryController.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update() modifies and returns updated category")
    void testUpdate_Success() {
        CategoryRequest req = new CategoryRequest("Roulements & Bagues", "Updated description");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByName("Roulements & Bagues")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryResponse res = categoryController.update(1L, req);

        assertThat(res.name()).isEqualTo("Roulements & Bagues");
        assertThat(res.description()).isEqualTo("Updated description");
        verify(categoryRepository).save(sampleCategory);
    }

    @Test
    @DisplayName("update() throws CONFLICT when renaming to existing name")
    void testUpdate_Conflict() {
        CategoryRequest req = new CategoryRequest("Outillage", "Rename attempt");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByName("Outillage")).thenReturn(true);

        assertThatThrownBy(() -> categoryController.update(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete() deletes category with 204 No Content")
    void testDelete_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

        ResponseEntity<Void> res = categoryController.delete(1L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(categoryRepository).delete(sampleCategory);
    }

    @Test
    @DisplayName("export() returns CSV file attachment")
    void testExport() {
        byte[] csv = "name,description\nRoulements,Desc\n".getBytes(StandardCharsets.UTF_8);
        when(importExportService.exportCategories()).thenReturn(csv);

        ResponseEntity<byte[]> res = categoryController.export();

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(csv);
        assertThat(res.getHeaders().getContentDisposition().getFilename()).isEqualTo("categories.csv");
    }

    @Test
    @DisplayName("importCsv() invokes service and returns result")
    void testImportCsv() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "cat.csv", "text/csv", "name,description\n".getBytes());
        ImportResultResponse expected = new ImportResultResponse(1, 0, 0, List.of(), List.of());
        when(importExportService.importCategories(file)).thenReturn(expected);

        ImportResultResponse res = categoryController.importCsv(file);

        assertThat(res.created()).isEqualTo(1);
        verify(importExportService).importCategories(file);
    }
}
