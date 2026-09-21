package com.novaerp.backend.config;

import com.novaerp.backend.client.Client;
import com.novaerp.backend.client.ClientRepository;
import com.novaerp.backend.invoices.CustomerInvoice;
import com.novaerp.backend.invoices.CustomerInvoiceItem;
import com.novaerp.backend.invoices.CustomerInvoiceRepository;
import com.novaerp.backend.invoices.CustomerInvoiceStatus;
import com.novaerp.backend.payments.Payment;
import com.novaerp.backend.payments.PaymentMethod;
import com.novaerp.backend.payments.PaymentRepository;
import com.novaerp.backend.payments.PaymentType;
import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderItem;
import com.novaerp.backend.purchases.PurchaseOrderRepository;
import com.novaerp.backend.purchases.PurchaseOrderStatus;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderItem;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.sales.SaleOrderStatus;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ClientRepository clientRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final PaymentRepository paymentRepository;
    private final StockTransferRepository stockTransferRepository;
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
        seedWarehouseStocks(articles);
        seedArticleSupplierPrices(articles, suppliers);
        seedStockMovements(articles, users);

        List<Client> clients = seedClients();
        List<PurchaseOrder> purchaseOrders = seedPurchaseOrders(suppliers, articles, users);
        List<SaleOrder> saleOrders = seedSaleOrders(clients, articles, users);
        List<CustomerInvoice> customerInvoices = seedCustomerInvoices(clients, saleOrders, users);
        List<Payment> customerPayments = seedCustomerPayments(customerInvoices, users);
        seedStockTransfers(articles, users);

        log.info("Seed data created: {} users, {} categories, {} units, {} suppliers, {} articles, " +
                "{} clients, {} purchase orders, {} sale orders, {} customer invoices, {} payments",
                users.size(), categories.size(), units.size(), suppliers.size(), articles.size(),
                clients.size(), purchaseOrders.size(), saleOrders.size(), customerInvoices.size(), customerPayments.size());
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

    private Warehouse resolveDefaultWarehouse() {
        return warehouseRepository.findByCode("WH-MAIN")
                .or(warehouseRepository::findByIsDefaultTrue)
                .orElse(null);
    }

    private WarehouseLocation resolveDefaultLocation(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }
        return warehouseLocationRepository.findByWarehouseIdAndCode(warehouse.getId(), "LOC-GEN")
                .or(() -> warehouseLocationRepository.findByWarehouseIdAndIsDefaultTrue(warehouse.getId()))
                .orElse(null);
    }

    private void seedWarehouseStocks(List<Article> articles) {
        Warehouse warehouse = resolveDefaultWarehouse();
        if (warehouse == null) {
            log.warn("Default warehouse WH-MAIN not found; skipping warehouse_stocks seeding");
            return;
        }

        WarehouseLocation location = resolveDefaultLocation(warehouse);
        if (location == null) {
            log.warn("Default location LOC-GEN not found for warehouse {}; skipping warehouse_stocks seeding", warehouse.getCode());
            return;
        }

        List<WarehouseStock> stocksToSave = new ArrayList<>();
        for (Article article : articles) {
            if (warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(
                    article.getId(), warehouse.getId(), location.getId()).isEmpty()) {
                stocksToSave.add(WarehouseStock.builder()
                        .article(article)
                        .warehouse(warehouse)
                        .location(location)
                        .quantity(article.getStockQuantity())
                        .minQuantity(article.getMinStockQuantity())
                        .build());
            }
        }

        if (!stocksToSave.isEmpty()) {
            warehouseStockRepository.saveAll(stocksToSave);
            log.info("Seeded {} warehouse_stocks records for warehouse '{}' / location '{}'",
                    stocksToSave.size(), warehouse.getCode(), location.getCode());
        }
    }

    private void seedStockMovements(List<Article> articles, List<User> users) {
        User admin = users.get(0);
        User sara = users.get(1);

        Warehouse warehouse = resolveDefaultWarehouse();
        WarehouseLocation location = resolveDefaultLocation(warehouse);

        List<StockMovement> movements = List.of(
                StockMovement.builder()
                        .article(articles.get(0))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("200.0000"))
                        .reference("PO-2026-0001")
                        .note("Initial stock intake")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(0))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("50.0000"))
                        .reference("SO-2026-0010")
                        .note("Customer order fulfillment")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(1))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("80.0000"))
                        .reference("PO-2026-0002")
                        .note("Initial stock intake")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(1))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("20.0000"))
                        .reference("SO-2026-0011")
                        .note("Customer order fulfillment")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(2))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("20.0000"))
                        .reference("PO-2026-0003")
                        .note("Initial stock intake")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(2))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("5.0000"))
                        .reference("SO-2026-0012")
                        .note("Installed at client site")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(4))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("350.0000"))
                        .reference("PO-2026-0004")
                        .note("Bulk office supplies restock")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(4))
                        .type(StockMovementType.ADJUSTMENT)
                        .quantity(new BigDecimal("-50.0000"))
                        .reference("ADJ-2026-0001")
                        .note("Inventory count correction")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(6))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("30.0000"))
                        .reference("PO-2026-0005")
                        .note("Initial stock intake")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(6))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("5.0000"))
                        .reference("SO-2026-0013")
                        .note("Customer order fulfillment")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(sara)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(8))
                        .type(StockMovementType.IN)
                        .quantity(new BigDecimal("10.0000"))
                        .reference("PO-2026-0006")
                        .note("Initial stock intake")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(admin)
                        .build(),
                StockMovement.builder()
                        .article(articles.get(8))
                        .type(StockMovementType.OUT)
                        .quantity(new BigDecimal("2.0000"))
                        .reference("SO-2026-0014")
                        .note("Customer order fulfillment")
                        .warehouse(warehouse)
                        .location(location)
                        .createdBy(sara)
                        .build()
        );
        stockMovementRepository.saveAll(movements);
    }

    private List<Client> seedClients() {
        List<Client> clients = List.of(
                Client.builder()
                        .name("Centrale BTP & Logistique SARL")
                        .email("contact@centrale-btp.ma")
                        .phone("+212522245678")
                        .address("125 Boulevard Abdelmoumen, 4ème étage")
                        .city("Casablanca")
                        .taxNumber("IF-28491032")
                        .notes("Grand compte BTP et fournitures industrielles")
                        .build(),
                Client.builder()
                        .name("Maghreb Solutions Informatiques")
                        .email("achats@maghrebsolutions.ma")
                        .phone("+212537684321")
                        .address("45 Avenue Fal Ould Oumeir, Agdal")
                        .city("Rabat")
                        .taxNumber("IF-49201834")
                        .notes("Intégrateur IT & Réseaux d'entreprise")
                        .build(),
                Client.builder()
                        .name("Tanger Logistique Express")
                        .email("logistique@tanger-express.ma")
                        .phone("+212539345678")
                        .address("Zone Franche d'Exportation, Ilot 12")
                        .city("Tanger")
                        .taxNumber("IF-67123984")
                        .notes("Opérateur logistique régional zone Nord")
                        .build()
        );
        return clientRepository.saveAll(clients);
    }

    private List<PurchaseOrder> seedPurchaseOrders(List<Supplier> suppliers, List<Article> articles, List<User> users) {
        User admin = users.get(0);
        User sara = users.get(1);
        Warehouse warehouse = resolveDefaultWarehouse();
        WarehouseLocation location = resolveDefaultLocation(warehouse);

        Supplier techImport = suppliers.get(1);
        Supplier maghrebOffice = suppliers.get(2);

        Instant now = Instant.now();

        // 1. PO-2026-0001: RECEIVED (matches initial intake movement PO-2026-0001 of 200 units of ELEC-0001)
        PurchaseOrder po1 = PurchaseOrder.builder()
                .orderNumber("PO-2026-0001")
                .supplier(techImport)
                .warehouse(warehouse)
                .location(location)
                .status(PurchaseOrderStatus.RECEIVED)
                .subtotalHt(new BigDecimal("17000.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("3400.0000"))
                .totalTtc(new BigDecimal("20400.0000"))
                .notes("Approvisionnement initial souris sans fil - Réceptionné conforme")
                .createdBy(admin)
                .createdAt(now.minus(Duration.ofDays(15)))
                .confirmedAt(now.minus(Duration.ofDays(14)))
                .receivedAt(now.minus(Duration.ofDays(10)))
                .items(new ArrayList<>())
                .build();

        po1.addItem(PurchaseOrderItem.builder()
                .article(articles.get(0))
                .quantity(new BigDecimal("200.0000"))
                .unitPrice(new BigDecimal("85.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("17000.0000"))
                .totalTtc(new BigDecimal("20400.0000"))
                .build());

        // 2. PO-2026-0101: CONFIRMED (in transit / pending reception)
        PurchaseOrder po2 = PurchaseOrder.builder()
                .orderNumber("PO-2026-0101")
                .supplier(maghrebOffice)
                .warehouse(warehouse)
                .location(location)
                .status(PurchaseOrderStatus.CONFIRMED)
                .subtotalHt(new BigDecimal("3500.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("700.0000"))
                .totalTtc(new BigDecimal("4200.0000"))
                .notes("Commande réapprovisionnement papier A4 - En cours d'acheminement")
                .createdBy(sara)
                .createdAt(now.minus(Duration.ofDays(3)))
                .confirmedAt(now.minus(Duration.ofDays(2)))
                .items(new ArrayList<>())
                .build();

        po2.addItem(PurchaseOrderItem.builder()
                .article(articles.get(4))
                .quantity(new BigDecimal("100.0000"))
                .unitPrice(new BigDecimal("35.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("3500.0000"))
                .totalTtc(new BigDecimal("4200.0000"))
                .build());

        return purchaseOrderRepository.saveAll(List.of(po1, po2));
    }

    private List<SaleOrder> seedSaleOrders(List<Client> clients, List<Article> articles, List<User> users) {
        User sara = users.get(1);
        Warehouse warehouse = resolveDefaultWarehouse();
        WarehouseLocation location = resolveDefaultLocation(warehouse);

        Instant now = Instant.now();

        // 1. SO-2026-0010: DELIVERED (matches customer order fulfillment SO-2026-0010 of 50 units of ELEC-0001)
        SaleOrder soDelivered = SaleOrder.builder()
                .orderNumber("SO-2026-0010")
                .client(clients.get(0))
                .warehouse(warehouse)
                .location(location)
                .status(SaleOrderStatus.DELIVERED)
                .subtotalHt(new BigDecimal("6450.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("1290.0000"))
                .totalTtc(new BigDecimal("7740.0000"))
                .notes("Équipement informatique postes de travail - Livré et réceptionné client")
                .createdBy(sara)
                .createdAt(now.minus(Duration.ofDays(7)))
                .confirmedAt(now.minus(Duration.ofDays(6)))
                .deliveredAt(now.minus(Duration.ofDays(3)))
                .items(new ArrayList<>())
                .build();

        soDelivered.addItem(SaleOrderItem.builder()
                .article(articles.get(0))
                .quantity(new BigDecimal("50.0000"))
                .unitPrice(new BigDecimal("129.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("6450.0000"))
                .totalTtc(new BigDecimal("7740.0000"))
                .build());

        // 2. SO-2026-0020: DRAFT (pending commercial quote)
        SaleOrder soDraft = SaleOrder.builder()
                .orderNumber("SO-2026-0020")
                .client(clients.get(1))
                .warehouse(warehouse)
                .location(location)
                .status(SaleOrderStatus.DRAFT)
                .subtotalHt(new BigDecimal("6093.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("1218.6000"))
                .totalTtc(new BigDecimal("7311.6000"))
                .notes("Devis pour claviers mécaniques et switch Gigabit")
                .createdBy(sara)
                .createdAt(now.minus(Duration.ofDays(1)))
                .items(new ArrayList<>())
                .build();

        soDraft.addItem(SaleOrderItem.builder()
                .article(articles.get(1))
                .quantity(new BigDecimal("5.0000"))
                .unitPrice(new BigDecimal("499.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("2495.0000"))
                .totalTtc(new BigDecimal("2994.0000"))
                .build());

        soDraft.addItem(SaleOrderItem.builder()
                .article(articles.get(2))
                .quantity(new BigDecimal("2.0000"))
                .unitPrice(new BigDecimal("1799.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("3598.0000"))
                .totalTtc(new BigDecimal("4317.6000"))
                .build());

        return saleOrderRepository.saveAll(List.of(soDelivered, soDraft));
    }

    private List<CustomerInvoice> seedCustomerInvoices(List<Client> clients, List<SaleOrder> saleOrders, List<User> users) {
        User sara = users.get(1);
        SaleOrder soDelivered = saleOrders.get(0);
        Instant now = Instant.now();

        CustomerInvoice invoice = CustomerInvoice.builder()
                .invoiceNumber("FAC-2026-0001")
                .client(clients.get(0))
                .saleOrder(soDelivered)
                .status(CustomerInvoiceStatus.ISSUED)
                .subtotalHt(new BigDecimal("6450.0000"))
                .taxRate(new BigDecimal("20.00"))
                .taxAmount(new BigDecimal("1290.0000"))
                .totalTtc(new BigDecimal("7740.0000"))
                .notes("Facture relative à la commande SO-2026-0010")
                .createdBy(sara)
                .createdAt(now.minus(Duration.ofDays(3)))
                .issuedAt(now.minus(Duration.ofDays(2)))
                .items(new ArrayList<>())
                .build();

        invoice.addItem(CustomerInvoiceItem.builder()
                .article(soDelivered.getItems().get(0).getArticle())
                .quantity(new BigDecimal("50.0000"))
                .unitPrice(new BigDecimal("129.0000"))
                .taxRate(new BigDecimal("20.00"))
                .totalHt(new BigDecimal("6450.0000"))
                .totalTtc(new BigDecimal("7740.0000"))
                .build());

        return customerInvoiceRepository.saveAll(List.of(invoice));
    }

    private List<Payment> seedCustomerPayments(List<CustomerInvoice> customerInvoices, List<User> users) {
        User sara = users.get(1);
        CustomerInvoice invoice = customerInvoices.get(0);
        Instant now = Instant.now();

        Payment payment = Payment.builder()
                .paymentNumber("PAY-2026-0001")
                .paymentType(PaymentType.CUSTOMER_PAYMENT)
                .customerInvoice(invoice)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(new BigDecimal("4000.0000"))
                .referenceNumber("VIR-BMCE-992014")
                .paymentDate(now.minus(Duration.ofDays(1)))
                .createdAt(now.minus(Duration.ofDays(1)))
                .createdBy(sara)
                .notes("Acompte client 4 000 MAD reçu par virement bancaire BMCE")
                .build();

        return paymentRepository.saveAll(List.of(payment));
    }

    private Warehouse resolveOrSeedSecondaryWarehouse() {
        return warehouseRepository.findByCode("WH-NORTH")
                .orElseGet(() -> warehouseRepository.save(Warehouse.builder()
                        .code("WH-NORTH")
                        .name("Entrepôt Régional Tanger")
                        .description("Hub logistique Zone Franche Tanger Med")
                        .address("Zone Franche, Tanger")
                        .active(true)
                        .isDefault(false)
                        .build()));
    }

    private WarehouseLocation resolveOrSeedSecondaryLocation(Warehouse warehouse) {
        return warehouseLocationRepository.findByWarehouseIdAndCode(warehouse.getId(), "LOC-NORTH-01")
                .orElseGet(() -> warehouseLocationRepository.save(WarehouseLocation.builder()
                        .warehouse(warehouse)
                        .code("LOC-NORTH-01")
                        .name("Zone Principale Tanger")
                        .description("Zone de stockage générale Tanger")
                        .active(true)
                        .isDefault(true)
                        .build()));
    }

    private StockTransfer seedStockTransfers(List<Article> articles, List<User> users) {
        Warehouse srcWarehouse = resolveDefaultWarehouse();
        if (srcWarehouse == null) {
            log.warn("Default warehouse WH-MAIN not found; skipping stock transfer seeding");
            return null;
        }

        WarehouseLocation srcLocation = resolveDefaultLocation(srcWarehouse);
        if (srcLocation == null) {
            log.warn("Default location LOC-GEN not found; skipping stock transfer seeding");
            return null;
        }

        Warehouse dstWarehouse = resolveOrSeedSecondaryWarehouse();
        WarehouseLocation dstLocation = resolveOrSeedSecondaryLocation(dstWarehouse);

        User admin = users.get(0);
        Article articleToTransfer = articles.get(0);
        BigDecimal transferQty = new BigDecimal("10.0000");

        // 1. Adjust source warehouse stock (-10)
        warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(
                articleToTransfer.getId(), srcWarehouse.getId(), srcLocation.getId())
                .ifPresent(srcStock -> {
                    srcStock.setQuantity(srcStock.getQuantity().subtract(transferQty));
                    warehouseStockRepository.save(srcStock);
                });

        // 2. Adjust or create destination warehouse stock (+10)
        WarehouseStock dstStock = warehouseStockRepository.findByArticleIdAndWarehouseIdAndLocationId(
                articleToTransfer.getId(), dstWarehouse.getId(), dstLocation.getId())
                .orElseGet(() -> WarehouseStock.builder()
                        .article(articleToTransfer)
                        .warehouse(dstWarehouse)
                        .location(dstLocation)
                        .quantity(BigDecimal.ZERO)
                        .minQuantity(BigDecimal.ZERO)
                        .build());
        dstStock.setQuantity(dstStock.getQuantity().add(transferQty));
        warehouseStockRepository.save(dstStock);

        // 3. Record linked OUT and IN movements for transfer ledger
        StockMovement outMovement = StockMovement.builder()
                .article(articleToTransfer)
                .type(StockMovementType.OUT)
                .quantity(transferQty)
                .reference("TRF-2026-0001")
                .note("Transfert vers " + dstWarehouse.getCode() + "/" + dstLocation.getCode())
                .warehouse(srcWarehouse)
                .location(srcLocation)
                .createdBy(admin)
                .build();

        StockMovement inMovement = StockMovement.builder()
                .article(articleToTransfer)
                .type(StockMovementType.IN)
                .quantity(transferQty)
                .reference("TRF-2026-0001")
                .note("Transfert depuis " + srcWarehouse.getCode() + "/" + srcLocation.getCode())
                .warehouse(dstWarehouse)
                .location(dstLocation)
                .createdBy(admin)
                .build();

        stockMovementRepository.saveAll(List.of(outMovement, inMovement));

        // 4. Save COMPLETED StockTransfer
        Instant transferTime = Instant.now().minus(Duration.ofDays(4));
        StockTransfer transfer = StockTransfer.builder()
                .transferNumber("TRF-2026-0001")
                .sourceWarehouse(srcWarehouse)
                .sourceLocation(srcLocation)
                .destinationWarehouse(dstWarehouse)
                .destinationLocation(dstLocation)
                .status(StockTransferStatus.COMPLETED)
                .notes("Transfert de réapprovisionnement vers l'antenne Tanger")
                .createdBy(admin)
                .createdAt(transferTime)
                .completedAt(transferTime)
                .items(new ArrayList<>())
                .build();

        transfer.addItem(StockTransferItem.builder()
                .article(articleToTransfer)
                .quantity(transferQty)
                .build());

        return stockTransferRepository.save(transfer);
    }
}
