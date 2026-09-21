package com.novaerp.backend.stock;

import com.novaerp.backend.common.csv.CsvResponses;
import com.novaerp.backend.stock.dto.ArticleRequest;
import com.novaerp.backend.stock.dto.ArticleResponse;
import com.novaerp.backend.stock.dto.ArticleSupplierPriceRequest;
import com.novaerp.backend.stock.dto.ArticleSupplierPriceResponse;
import com.novaerp.backend.stock.dto.ImportResultResponse;
import com.novaerp.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stock/articles")
@RequiredArgsConstructor
@Tag(name = "Stock - Articles", description = "Article (stock item) management")
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleSupplierPriceRepository articleSupplierPriceRepository;
    private final StockImportExportService importExportService;

    @GetMapping
    @Operation(summary = "List all articles with optional search, category or low-stock filtering")
    public Page<ArticleResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        boolean hasFilter = trimmedSearch != null || categoryId != null || Boolean.TRUE.equals(lowStock);

        if (!hasFilter) {
            return articleRepository.findAll(pageable).map(ArticleResponse::from);
        }

        return articleRepository.searchArticles(
                trimmedSearch,
                categoryId,
                Boolean.TRUE.equals(lowStock) ? true : null,
                pageable
        ).map(ArticleResponse::from);
    }

    public Page<ArticleResponse> list(Pageable pageable) {
        return list(null, null, null, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an article by id")
    public ArticleResponse get(@PathVariable Long id) {
        return ArticleResponse.from(findOrThrow(id));
    }

    @PostMapping
    @Operation(summary = "Create an article")
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        if (articleRepository.existsByReference(request.reference())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article reference already exists");
        }

        Article article = Article.builder()
                .reference(request.reference())
                .designation(request.designation())
                .brand(request.brand())
                .barcode(request.barcode())
                .category(resolveCategory(request.categoryId()))
                .unit(resolveUnit(request.unitId()))
                .purchasePriceHt(request.purchasePriceHt())
                .unitCostTtc(request.unitCostTtc())
                .salePriceHt(request.salePriceHt())
                .minStockQuantity(request.minStockQuantity())
                .stockQuantity(BigDecimal.ZERO)
                .serialTracked(request.serialTracked())
                .description(request.description())
                .notes(request.notes())
                .build();

        articleRepository.save(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.from(article));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an article")
    public ArticleResponse update(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        Article article = findOrThrow(id);

        if (!article.getReference().equals(request.reference()) && articleRepository.existsByReference(request.reference())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article reference already exists");
        }

        article.setReference(request.reference());
        article.setDesignation(request.designation());
        article.setBrand(request.brand());
        article.setBarcode(request.barcode());
        article.setCategory(resolveCategory(request.categoryId()));
        article.setUnit(resolveUnit(request.unitId()));
        article.setPurchasePriceHt(request.purchasePriceHt());
        article.setUnitCostTtc(request.unitCostTtc());
        article.setSalePriceHt(request.salePriceHt());
        article.setMinStockQuantity(request.minStockQuantity());
        article.setSerialTracked(request.serialTracked());
        article.setDescription(request.description());
        article.setNotes(request.notes());

        articleRepository.save(article);
        return ArticleResponse.from(article);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an article")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Article article = findOrThrow(id);
        articleRepository.delete(article);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Export articles as CSV with optional filtering")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean lowStock) {
        return CsvResponses.attachment(importExportService.exportArticles(search, categoryId, lowStock), "articles.csv");
    }

    @PostMapping("/import")
    @Operation(summary = "Import articles from a CSV file (columns: reference, designation, brand, barcode, "
            + "category, unit, purchasePriceHt, unitCostTtc, salePriceHt, stockQuantity, minStockQuantity, "
            + "serialTracked, description, notes, primarySupplier)")
    public ImportResultResponse importCsv(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user)
            throws IOException {
        return importExportService.importArticles(file, user);
    }

    @GetMapping("/{id}/supplier-prices")
    @Operation(summary = "List supplier prices for an article")
    public List<ArticleSupplierPriceResponse> listSupplierPrices(@PathVariable Long id) {
        findOrThrow(id);
        return articleSupplierPriceRepository.findByArticleId(id).stream()
                .map(ArticleSupplierPriceResponse::from)
                .toList();
    }

    @PostMapping("/{id}/supplier-prices")
    @Operation(summary = "Add a supplier price to an article")
    public ResponseEntity<ArticleSupplierPriceResponse> addSupplierPrice(
            @PathVariable Long id, @Valid @RequestBody ArticleSupplierPriceRequest request) {
        Article article = findOrThrow(id);
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        boolean primary = Boolean.TRUE.equals(request.primary());
        if (primary) {
            clearPrimary(id);
        }

        ArticleSupplierPrice price = ArticleSupplierPrice.builder()
                .article(article)
                .supplier(supplier)
                .primary(primary)
                .currency(request.currency() != null ? request.currency() : "MAD")
                .priceHt(request.priceHt())
                .taxRate(request.taxRate() != null ? request.taxRate() : BigDecimal.ZERO)
                .priceTtc(request.priceTtc())
                .leadTimeDays(request.leadTimeDays())
                .quoteDate(request.quoteDate())
                .build();

        articleSupplierPriceRepository.save(price);
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleSupplierPriceResponse.from(price));
    }

    @PutMapping("/{id}/supplier-prices/{priceId}/primary")
    @Operation(summary = "Mark a supplier price as the article's primary supplier")
    public ArticleSupplierPriceResponse setPrimarySupplierPrice(@PathVariable Long id, @PathVariable Long priceId) {
        findOrThrow(id);
        ArticleSupplierPrice target = articleSupplierPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier price not found"));

        if (!target.getArticle().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier price not found");
        }

        clearPrimary(id);
        target.setPrimary(true);
        articleSupplierPriceRepository.save(target);
        return ArticleSupplierPriceResponse.from(target);
    }

    private void clearPrimary(Long articleId) {
        articleSupplierPriceRepository.findByArticleId(articleId).forEach(existing -> {
            if (existing.isPrimary()) {
                existing.setPrimary(false);
                articleSupplierPriceRepository.save(existing);
            }
        });
    }

    @DeleteMapping("/{id}/supplier-prices/{priceId}")
    @Operation(summary = "Remove a supplier price from an article")
    public ResponseEntity<Void> deleteSupplierPrice(@PathVariable Long id, @PathVariable Long priceId) {
        ArticleSupplierPrice price = articleSupplierPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier price not found"));

        if (!price.getArticle().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier price not found");
        }

        articleSupplierPriceRepository.delete(price);
        return ResponseEntity.noContent().build();
    }

    private Article findOrThrow(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private Unit resolveUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));
    }
}
