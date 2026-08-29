package com.novaerp.backend.stock;

import com.novaerp.backend.common.csv.CsvUtils;
import com.novaerp.backend.stock.dto.ImportResultResponse;
import com.novaerp.backend.stock.dto.ImportRowIssue;
import com.novaerp.backend.stock.dto.StockMovementRequest;
import com.novaerp.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV export/import for the three stock reference lists (categories,
 * suppliers, articles). Imports are idempotent: a row whose reference/name
 * already exists is skipped rather than duplicated or overwritten, so the
 * same file can safely be re-uploaded (e.g. after fixing a few rows).
 */
@Service
@RequiredArgsConstructor
public class StockImportExportService {

    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final UnitRepository unitRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSupplierPriceRepository articleSupplierPriceRepository;
    private final StockMovementService stockMovementService;

    // ---------------------------------------------------------------- categories

    public byte[] exportCategories() {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.row("name", "description"));
        for (Category category : categoryRepository.findAll()) {
            sb.append(CsvUtils.row(category.getName(), category.getDescription()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ImportResultResponse importCategories(MultipartFile file) throws IOException {
        List<String[]> rows = CsvUtils.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<ImportRowIssue> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 1;
            String name = CsvUtils.field(row, 0);
            String description = CsvUtils.field(row, 1);

            if (name.isEmpty()) {
                failed++;
                errors.add(new ImportRowIssue(rowNum, "", "Name is required"));
                continue;
            }
            if (categoryRepository.existsByName(name)) {
                skipped++;
                continue;
            }

            categoryRepository.save(Category.builder()
                    .name(name)
                    .description(description.isEmpty() ? null : description)
                    .build());
            created++;
        }

        return new ImportResultResponse(created, skipped, failed, errors, List.of());
    }

    // ----------------------------------------------------------------- suppliers

    public byte[] exportSuppliers() {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.row("name", "email", "phone", "address"));
        for (Supplier supplier : supplierRepository.findAll()) {
            sb.append(CsvUtils.row(supplier.getName(), supplier.getEmail(), supplier.getPhone(), supplier.getAddress()));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ImportResultResponse importSuppliers(MultipartFile file) throws IOException {
        List<String[]> rows = CsvUtils.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<ImportRowIssue> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 1;
            String name = CsvUtils.field(row, 0);

            if (name.isEmpty()) {
                failed++;
                errors.add(new ImportRowIssue(rowNum, "", "Name is required"));
                continue;
            }
            if (supplierRepository.existsByName(name)) {
                skipped++;
                continue;
            }

            supplierRepository.save(Supplier.builder()
                    .name(name)
                    .email(blankToNull(CsvUtils.field(row, 1)))
                    .phone(blankToNull(CsvUtils.field(row, 2)))
                    .address(blankToNull(CsvUtils.field(row, 3)))
                    .build());
            created++;
        }

        return new ImportResultResponse(created, skipped, failed, errors, List.of());
    }

    // ------------------------------------------------------------------ articles

    private static final int COL_REFERENCE = 0;
    private static final int COL_DESIGNATION = 1;
    private static final int COL_BRAND = 2;
    private static final int COL_BARCODE = 3;
    private static final int COL_CATEGORY = 4;
    private static final int COL_UNIT = 5;
    private static final int COL_PURCHASE_PRICE = 6;
    private static final int COL_UNIT_COST_TTC = 7;
    private static final int COL_SALE_PRICE = 8;
    private static final int COL_STOCK = 9;
    private static final int COL_MIN_STOCK = 10;
    private static final int COL_SERIAL_TRACKED = 11;
    private static final int COL_DESCRIPTION = 12;
    private static final int COL_NOTES = 13;
    private static final int COL_PRIMARY_SUPPLIER = 14;

    public byte[] exportArticles() {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.row("reference", "designation", "brand", "barcode", "category", "unit",
                "purchasePriceHt", "unitCostTtc", "salePriceHt", "stockQuantity", "minStockQuantity",
                "serialTracked", "description", "notes", "primarySupplier"));

        for (Article article : articleRepository.findAll()) {
            String primarySupplier = articleSupplierPriceRepository.findByArticleId(article.getId()).stream()
                    .filter(ArticleSupplierPrice::isPrimary)
                    .map(p -> p.getSupplier().getName())
                    .findFirst()
                    .orElse("");

            sb.append(CsvUtils.row(
                    article.getReference(),
                    article.getDesignation(),
                    article.getBrand(),
                    article.getBarcode(),
                    article.getCategory() != null ? article.getCategory().getName() : "",
                    article.getUnit() != null ? article.getUnit().getName() : "",
                    article.getPurchasePriceHt(),
                    article.getUnitCostTtc(),
                    article.getSalePriceHt(),
                    article.getStockQuantity(),
                    article.getMinStockQuantity(),
                    article.isSerialTracked(),
                    article.getDescription(),
                    article.getNotes(),
                    primarySupplier
            ));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ImportResultResponse importArticles(MultipartFile file, User createdBy) throws IOException {
        List<String[]> rows = CsvUtils.parse(new String(file.getBytes(), StandardCharsets.UTF_8));
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<ImportRowIssue> errors = new ArrayList<>();
        List<ImportRowIssue> warnings = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            int rowNum = i + 1;
            String reference = CsvUtils.field(row, COL_REFERENCE);

            if (reference.isEmpty()) {
                failed++;
                errors.add(new ImportRowIssue(rowNum, "", "Reference is required"));
                continue;
            }
            if (articleRepository.existsByReference(reference)) {
                skipped++;
                continue;
            }

            String designation = CsvUtils.field(row, COL_DESIGNATION);
            if (designation.isEmpty()) {
                designation = reference;
                warnings.add(new ImportRowIssue(rowNum, reference, "Designation missing, used reference instead"));
            }

            BigDecimal purchasePrice = parseDecimal(row, COL_PURCHASE_PRICE, rowNum, reference, "purchasePriceHt", warnings);
            BigDecimal unitCostTtc = parseDecimalOrDefault(CsvUtils.field(row, COL_UNIT_COST_TTC), purchasePrice);
            BigDecimal salePrice = parseDecimal(row, COL_SALE_PRICE, rowNum, reference, "salePriceHt", warnings);
            BigDecimal minStockQuantity = parseDecimal(row, COL_MIN_STOCK, rowNum, reference, "minStockQuantity", warnings);

            BigDecimal stockQuantity = parseDecimal(row, COL_STOCK, rowNum, reference, "stockQuantity", warnings);
            if (stockQuantity.compareTo(BigDecimal.ZERO) < 0) {
                warnings.add(new ImportRowIssue(rowNum, reference,
                        "Negative stock (" + stockQuantity + ") imported as 0"));
                stockQuantity = BigDecimal.ZERO;
            }

            Category category = resolveOrCreateCategory(CsvUtils.field(row, COL_CATEGORY));
            Unit unit = resolveUnit(CsvUtils.field(row, COL_UNIT), rowNum, reference, warnings);

            Article article = Article.builder()
                    .reference(reference)
                    .designation(designation)
                    .brand(blankToNull(CsvUtils.field(row, COL_BRAND)))
                    .barcode(blankToNull(CsvUtils.field(row, COL_BARCODE)))
                    .category(category)
                    .unit(unit)
                    .purchasePriceHt(purchasePrice)
                    .unitCostTtc(unitCostTtc)
                    .salePriceHt(salePrice)
                    .stockQuantity(BigDecimal.ZERO)
                    .minStockQuantity(minStockQuantity)
                    .serialTracked(parseBoolean(CsvUtils.field(row, COL_SERIAL_TRACKED)))
                    .description(blankToNull(CsvUtils.field(row, COL_DESCRIPTION)))
                    .notes(blankToNull(CsvUtils.field(row, COL_NOTES)))
                    .build();
            articleRepository.save(article);
            created++;

            if (stockQuantity.compareTo(BigDecimal.ZERO) > 0) {
                stockMovementService.record(new StockMovementRequest(
                        article.getId(), StockMovementType.IN, stockQuantity,
                        "csv-import", "Initial stock from CSV import"), createdBy);
            }

            String primarySupplierName = CsvUtils.field(row, COL_PRIMARY_SUPPLIER);
            if (!primarySupplierName.isEmpty()) {
                Supplier supplier = supplierRepository.findByName(primarySupplierName)
                        .orElseGet(() -> supplierRepository.save(Supplier.builder().name(primarySupplierName).build()));
                articleSupplierPriceRepository.save(ArticleSupplierPrice.builder()
                        .article(article)
                        .supplier(supplier)
                        .primary(true)
                        .priceHt(purchasePrice)
                        .taxRate(BigDecimal.ZERO)
                        .priceTtc(purchasePrice)
                        .build());
            }
        }

        return new ImportResultResponse(created, skipped, failed, errors, warnings);
    }

    private Category resolveOrCreateCategory(String name) {
        if (name.isEmpty()) {
            return null;
        }
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(Category.builder().name(name).build()));
    }

    private Unit resolveUnit(String name, int rowNum, String reference, List<ImportRowIssue> warnings) {
        if (name.isEmpty()) {
            return null;
        }
        return unitRepository.findByName(name).orElseGet(() -> {
            warnings.add(new ImportRowIssue(rowNum, reference, "Unit '" + name + "' not found, imported without a unit"));
            return null;
        });
    }

    private static String blankToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static BigDecimal parseDecimal(String[] row, int col, int rowNum, String identifier, String field,
                                            List<ImportRowIssue> warnings) {
        String raw = CsvUtils.field(row, col);
        if (raw.isEmpty()) {
            warnings.add(new ImportRowIssue(rowNum, identifier, field + " missing, defaulted to 0"));
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            warnings.add(new ImportRowIssue(rowNum, identifier, field + " '" + raw + "' is not a number, defaulted to 0"));
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parseDecimalOrDefault(String raw, BigDecimal fallback) {
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw) {
        return raw.equalsIgnoreCase("true") || raw.equals("1");
    }
}
