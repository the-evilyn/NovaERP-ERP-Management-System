package com.novaerp.backend.config;

import com.novaerp.backend.stock.*;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Populates the database with demo data. Only runs with the "seed" profile
 * active (e.g. -Dspring.profiles.active=seed) and skips entirely if users
 * already exist, so it is safe to leave enabled across restarts.
 */
@Component
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleRepository articleRepository;
    private final ArticleSupplierPriceRepository articleSupplierPriceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed data skipped: users already exist");
            return;
        }

        log.info("Seeding demo data...");

        List<User> users = seedUsers();
        Map<String, Category> categories = seedCategories();
        Map<String, Unit> units = seedUnits();
        List<Supplier> suppliers = seedSuppliers();
        List<Article> articles = seedArticles(categories, units);
        seedArticleSupplierPrices(articles, suppliers);
        seedStockMovements(articles, users);

        log.info("Seed data created: {} users, {} categories, {} units, {} suppliers, {} articles",
                users.size(), categories.size(), units.size(), suppliers.size(), articles.size());
    }

    private List<User> seedUsers() {
        List<User> users = List.of(
                User.builder()
                        .fullName("Rida Ezziani")
                        .email("admin@novaerp.local")
                        .password(passwordEncoder.encode("Admin123!"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build(),
                User.builder()
                        .fullName("Sara Amrani")
                        .email("sara.amrani@novaerp.local")
                        .password(passwordEncoder.encode("User1234!"))
                        .role(Role.USER)
                        .enabled(true)
                        .build(),
                User.builder()
                        .fullName("Youssef Bennani")
                        .email("youssef.bennani@novaerp.local")
                        .password(passwordEncoder.encode("User1234!"))
                        .role(Role.USER)
                        .enabled(true)
                        .build()
        );
        return userRepository.saveAll(users);
    }

    private Map<String, Category> seedCategories() {
        List<Category> categories = List.of(
                Category.builder().name("Electronics").description("Electronic devices and components").build(),
                Category.builder().name("Office Supplies").description("Stationery and office equipment").build(),
                Category.builder().name("Furniture").description("Office and warehouse furniture").build(),
                Category.builder().name("Networking").description("Network hardware and cabling").build(),
                Category.builder().name("Tools").description("Hand and power tools").build()
        );
        List<Category> saved = categoryRepository.saveAll(categories);
        return saved.stream().collect(java.util.stream.Collectors.toMap(Category::getName, c -> c));
    }

    private Map<String, Unit> seedUnits() {
        List<Unit> units = List.of(
                Unit.builder().name("Piece").symbol("pc").build(),
                Unit.builder().name("Box").symbol("bx").build(),
                Unit.builder().name("Kilogram").symbol("kg").build(),
                Unit.builder().name("Liter").symbol("L").build(),
                Unit.builder().name("Meter").symbol("m").build()
        );
        List<Unit> saved = unitRepository.saveAll(units);
        return saved.stream().collect(java.util.stream.Collectors.toMap(Unit::getName, u -> u));
    }

    private List<Supplier> seedSuppliers() {
        List<Supplier> suppliers = List.of(
                Supplier.builder()
                        .name("Atlas Distribution")
                        .email("contact@atlasdist.ma")
                        .phone("+212522000111")
                        .address("Zone Industrielle Sidi Bernoussi, Casablanca")
                        .build(),
                Supplier.builder()
                        .name("TechImport SARL")
                        .email("sales@techimport.ma")
                        .phone("+212537123456")
                        .address("Rue Allal Ben Abdellah, Rabat")
                        .build(),
                Supplier.builder()
                        .name("Maghreb Office Supplies")
                        .email("info@maghrebsupplies.ma")
                        .phone("+212539654321")
                        .address("Boulevard Zerktouni, Tanger")
                        .build()
        );
        return supplierRepository.saveAll(suppliers);
    }

    private List<Article> seedArticles(Map<String, Category> categories, Map<String, Unit> units) {
        List<Article> articles = List.of(
                Article.builder()
                        .reference("ELEC-0001")
                        .designation("Wireless Mouse")
                        .brand("Logitech")
                        .barcode("6291000001011")
                        .category(categories.get("Electronics"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("85.0000"))
                        .unitCostTtc(new BigDecimal("102.0000"))
                        .salePriceHt(new BigDecimal("129.0000"))
                        .stockQuantity(new BigDecimal("150.0000"))
                        .minStockQuantity(new BigDecimal("20.0000"))
                        .serialTracked(false)
                        .description("Ergonomic 2.4GHz wireless mouse")
                        .build(),
                Article.builder()
                        .reference("ELEC-0002")
                        .designation("Mechanical Keyboard")
                        .brand("Logitech")
                        .barcode("6291000001028")
                        .category(categories.get("Electronics"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("320.0000"))
                        .unitCostTtc(new BigDecimal("384.0000"))
                        .salePriceHt(new BigDecimal("499.0000"))
                        .stockQuantity(new BigDecimal("60.0000"))
                        .minStockQuantity(new BigDecimal("10.0000"))
                        .serialTracked(true)
                        .description("RGB backlit mechanical keyboard")
                        .build(),
                Article.builder()
                        .reference("NET-0001")
                        .designation("24-Port Gigabit Switch")
                        .brand("TP-Link")
                        .barcode("6935364001234")
                        .category(categories.get("Networking"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("1200.0000"))
                        .unitCostTtc(new BigDecimal("1440.0000"))
                        .salePriceHt(new BigDecimal("1799.0000"))
                        .stockQuantity(new BigDecimal("15.0000"))
                        .minStockQuantity(new BigDecimal("3.0000"))
                        .serialTracked(true)
                        .description("Managed 24-port Gigabit Ethernet switch")
                        .build(),
                Article.builder()
                        .reference("NET-0002")
                        .designation("Ethernet Cable Cat6")
                        .brand("Generic")
                        .barcode("6935364001241")
                        .category(categories.get("Networking"))
                        .unit(units.get("Meter"))
                        .purchasePriceHt(new BigDecimal("2.5000"))
                        .unitCostTtc(new BigDecimal("3.0000"))
                        .salePriceHt(new BigDecimal("4.5000"))
                        .stockQuantity(new BigDecimal("500.0000"))
                        .minStockQuantity(new BigDecimal("100.0000"))
                        .serialTracked(false)
                        .description("Cat6 UTP cable, sold by the meter")
                        .build(),
                Article.builder()
                        .reference("OFF-0001")
                        .designation("A4 Paper Ream")
                        .brand("Clairefontaine")
                        .barcode("3329680123456")
                        .category(categories.get("Office Supplies"))
                        .unit(units.get("Box"))
                        .purchasePriceHt(new BigDecimal("35.0000"))
                        .unitCostTtc(new BigDecimal("42.0000"))
                        .salePriceHt(new BigDecimal("55.0000"))
                        .stockQuantity(new BigDecimal("300.0000"))
                        .minStockQuantity(new BigDecimal("50.0000"))
                        .serialTracked(false)
                        .description("500 sheets, 80g/m2, A4 format")
                        .build(),
                Article.builder()
                        .reference("OFF-0002")
                        .designation("Blue Ballpoint Pen Box")
                        .brand("Bic")
                        .barcode("3086123456789")
                        .category(categories.get("Office Supplies"))
                        .unit(units.get("Box"))
                        .purchasePriceHt(new BigDecimal("18.0000"))
                        .unitCostTtc(new BigDecimal("21.6000"))
                        .salePriceHt(new BigDecimal("29.0000"))
                        .stockQuantity(new BigDecimal("120.0000"))
                        .minStockQuantity(new BigDecimal("15.0000"))
                        .serialTracked(false)
                        .description("Box of 50 blue ballpoint pens")
                        .build(),
                Article.builder()
                        .reference("FUR-0001")
                        .designation("Ergonomic Office Chair")
                        .brand("IKEA")
                        .barcode("7350053850012")
                        .category(categories.get("Furniture"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("650.0000"))
                        .unitCostTtc(new BigDecimal("780.0000"))
                        .salePriceHt(new BigDecimal("999.0000"))
                        .stockQuantity(new BigDecimal("25.0000"))
                        .minStockQuantity(new BigDecimal("5.0000"))
                        .serialTracked(false)
                        .description("Adjustable ergonomic office chair")
                        .build(),
                Article.builder()
                        .reference("FUR-0002")
                        .designation("Standing Desk")
                        .brand("IKEA")
                        .barcode("7350053850029")
                        .category(categories.get("Furniture"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("1500.0000"))
                        .unitCostTtc(new BigDecimal("1800.0000"))
                        .salePriceHt(new BigDecimal("2299.0000"))
                        .stockQuantity(new BigDecimal("10.0000"))
                        .minStockQuantity(new BigDecimal("2.0000"))
                        .serialTracked(false)
                        .description("Electric height-adjustable standing desk")
                        .build(),
                Article.builder()
                        .reference("TL-0001")
                        .designation("Cordless Drill")
                        .brand("Bosch")
                        .barcode("3165140812345")
                        .category(categories.get("Tools"))
                        .unit(units.get("Piece"))
                        .purchasePriceHt(new BigDecimal("450.0000"))
                        .unitCostTtc(new BigDecimal("540.0000"))
                        .salePriceHt(new BigDecimal("699.0000"))
                        .stockQuantity(new BigDecimal("8.0000"))
                        .minStockQuantity(new BigDecimal("2.0000"))
                        .serialTracked(true)
                        .description("18V cordless drill with battery pack")
                        .build(),
                Article.builder()
                        .reference("TL-0002")
                        .designation("Machine Oil")
                        .brand("Total")
                        .barcode("3165140812352")
                        .category(categories.get("Tools"))
                        .unit(units.get("Liter"))
                        .purchasePriceHt(new BigDecimal("40.0000"))
                        .unitCostTtc(new BigDecimal("48.0000"))
                        .salePriceHt(new BigDecimal("65.0000"))
                        .stockQuantity(new BigDecimal("80.0000"))
                        .minStockQuantity(new BigDecimal("10.0000"))
                        .serialTracked(false)
                        .description("Multi-purpose machine lubricant")
                        .build()
        );
        return articleRepository.saveAll(articles);
    }

    private void seedArticleSupplierPrices(List<Article> articles, List<Supplier> suppliers) {
        Supplier atlas = suppliers.get(0);
        Supplier techImport = suppliers.get(1);
        Supplier maghrebOffice = suppliers.get(2);

        List<ArticleSupplierPrice> prices = List.of(
                priceOf(articles.get(0), techImport, "82.0000", "98.4000", 5, LocalDate.of(2026, 6, 1)),
                priceOf(articles.get(1), techImport, "310.0000", "372.0000", 5, LocalDate.of(2026, 6, 1)),
                priceOf(articles.get(2), techImport, "1150.0000", "1380.0000", 10, LocalDate.of(2026, 5, 15)),
                priceOf(articles.get(3), atlas, "2.3000", "2.7600", 3, LocalDate.of(2026, 7, 1)),
                priceOf(articles.get(4), maghrebOffice, "33.0000", "39.6000", 2, LocalDate.of(2026, 7, 10)),
                priceOf(articles.get(5), maghrebOffice, "17.0000", "20.4000", 2, LocalDate.of(2026, 7, 10)),
                priceOf(articles.get(6), atlas, "620.0000", "744.0000", 15, LocalDate.of(2026, 4, 20)),
                priceOf(articles.get(7), atlas, "1450.0000", "1740.0000", 20, LocalDate.of(2026, 4, 20)),
                priceOf(articles.get(8), techImport, "430.0000", "516.0000", 7, LocalDate.of(2026, 6, 25)),
                priceOf(articles.get(9), atlas, "38.0000", "45.6000", 4, LocalDate.of(2026, 7, 5))
        );
        articleSupplierPriceRepository.saveAll(prices);
    }

    private ArticleSupplierPrice priceOf(Article article, Supplier supplier, String priceHt, String priceTtc,
                                          int leadTimeDays, LocalDate quoteDate) {
        return ArticleSupplierPrice.builder()
                .article(article)
                .supplier(supplier)
                .currency("MAD")
                .priceHt(new BigDecimal(priceHt))
                .taxRate(new BigDecimal("20.00"))
                .priceTtc(new BigDecimal(priceTtc))
                .leadTimeDays(leadTimeDays)
                .quoteDate(quoteDate)
                .build();
    }

    private void seedStockMovements(List<Article> articles, List<User> users) {
        User admin = users.get(0);
        User sara = users.get(1);

        List<StockMovement> movements = List.of(
                StockMovement.builder()
                        .article(articles.get(0))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("200.0000"))
                        .reference("PO-2026-0001")
                        .note("Initial stock intake")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(0))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("50.0000"))
                        .reference("SO-2026-0010")
                        .note("Customer order fulfillment")
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(1))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("80.0000"))
                        .reference("PO-2026-0002")
                        .note("Initial stock intake")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(1))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("20.0000"))
                        .reference("SO-2026-0011")
                        .note("Customer order fulfillment")
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(2))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("20.0000"))
                        .reference("PO-2026-0003")
                        .note("Initial stock intake")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(2))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("5.0000"))
                        .reference("SO-2026-0012")
                        .note("Installed at client site")
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(4))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("350.0000"))
                        .reference("PO-2026-0004")
                        .note("Bulk office supplies restock")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(4))
                        .type(StockMovementType.ADJUSTMENT)
                        .quantity(new BigDecimal("-50.0000"))
                        .reference("ADJ-2026-0001")
                        .note("Inventory count correction")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(6))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("30.0000"))
                        .reference("PO-2026-0005")
                        .note("Initial stock intake")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(6))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("5.0000"))
                        .reference("SO-2026-0013")
                        .note("Customer order fulfillment")
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(8))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("10.0000"))
                        .reference("PO-2026-0006")
                        .note("Initial stock intake")
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(8))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("2.0000"))
                        .reference("SO-2026-0014")
                        .note("Customer order fulfillment")
                        .createdBy(sara)
                        .build()
        );
        stockMovementRepository.saveAll(movements);
    }
}
