package com.novaerp.backend.stock;

import com.novaerp.backend.common.csv.CsvResponses;
import com.novaerp.backend.stock.dto.CategoryRequest;
import com.novaerp.backend.stock.dto.CategoryResponse;
import com.novaerp.backend.stock.dto.ImportResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/api/stock/categories")
@RequiredArgsConstructor
@Tag(name = "Stock - Categories", description = "Article category management")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final StockImportExportService importExportService;

    @GetMapping
    @Operation(summary = "List all categories")
    public Page<CategoryResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return categoryRepository.findAll(pageable).map(CategoryResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    public CategoryResponse get(@PathVariable Long id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    @PostMapping
    @Operation(summary = "Create a category")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = findOrThrow(id);

        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Category category = findOrThrow(id);
        categoryRepository.delete(category);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Export all categories as CSV")
    public ResponseEntity<byte[]> export() {
        return CsvResponses.attachment(importExportService.exportCategories(), "categories.csv");
    }

    @PostMapping("/import")
    @Operation(summary = "Import categories from a CSV file (columns: name, description)")
    public ImportResultResponse importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return importExportService.importCategories(file);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
