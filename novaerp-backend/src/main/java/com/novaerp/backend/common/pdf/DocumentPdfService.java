package com.novaerp.backend.common.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.novaerp.backend.client.Client;
import com.novaerp.backend.invoices.CustomerInvoice;
import com.novaerp.backend.invoices.CustomerInvoiceItem;
import com.novaerp.backend.invoices.CustomerInvoiceRepository;
import com.novaerp.backend.invoices.SupplierInvoice;
import com.novaerp.backend.invoices.SupplierInvoiceItem;
import com.novaerp.backend.invoices.SupplierInvoiceRepository;
import com.novaerp.backend.payments.PaymentRepository;
import com.novaerp.backend.purchases.PurchaseOrder;
import com.novaerp.backend.purchases.PurchaseOrderItem;
import com.novaerp.backend.purchases.PurchaseOrderRepository;
import com.novaerp.backend.sales.SaleOrder;
import com.novaerp.backend.sales.SaleOrderItem;
import com.novaerp.backend.sales.SaleOrderRepository;
import com.novaerp.backend.stock.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DocumentPdfService {

    private final CustomerInvoiceRepository customerInvoiceRepository;
    private final SupplierInvoiceRepository supplierInvoiceRepository;
    private final SaleOrderRepository saleOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentRepository paymentRepository;

    private static final Color PRIMARY_COLOR = new Color(15, 23, 42); // slate-900
    private static final Color SECONDARY_TEXT = new Color(100, 116, 139); // slate-500
    private static final Color BORDER_COLOR = new Color(226, 232, 240); // slate-200
    private static final Color HEADER_BG = new Color(241, 245, 249); // slate-100
    private static final Color ALT_ROW_BG = new Color(248, 250, 252); // slate-50

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
    private static final Font FONT_HEADER_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, PRIMARY_COLOR);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);
    private static final Font FONT_REGULAR = FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR);
    private static final Font FONT_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8, SECONDARY_TEXT);
    private static final Font FONT_MUTED_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, SECONDARY_TEXT);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARY_COLOR);
    private static final Font FONT_TABLE_ROW = FontFactory.getFont(FontFactory.HELVETICA, 8, PRIMARY_COLOR);
    private static final Font FONT_TABLE_ROW_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARY_COLOR);
    private static final Font FONT_TOTAL_TITLE = FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR);
    private static final Font FONT_TOTAL_VAL = FontFactory.getFont(FontFactory.HELVETICA, 9, PRIMARY_COLOR);
    private static final Font FONT_TOTAL_MAIN_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);
    private static final Font FONT_TOTAL_MAIN_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR);

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    public record PdfDocument(byte[] content, String filename) {}

    @Transactional(readOnly = true)
    public PdfDocument generateCustomerInvoicePdf(Long id) {
        CustomerInvoice invoice = customerInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture client introuvable: ID " + id));

        BigDecimal paidAmount = paymentRepository.sumAmountByCustomerInvoiceId(id);
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        BigDecimal totalTtc = invoice.getTotalTtc() != null ? invoice.getTotalTtc() : BigDecimal.ZERO;
        BigDecimal balanceDue = totalTtc.subtract(paidAmount).max(BigDecimal.ZERO);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 45);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageNumberEvent());
        document.open();

        // 1. Header Banner
        addHeader(document, "FACTURE CLIENT", invoice.getInvoiceNumber(), invoice.getStatus().name());

        // 2. Metadata & Client Info
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        // Left box: Document details
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.BOX);
        metaCell.setBorderColor(BORDER_COLOR);
        metaCell.setPadding(8);
        metaCell.setBackgroundColor(ALT_ROW_BG);
        metaCell.addElement(new Paragraph("DÉTAILS DU DOCUMENT", FONT_SECTION));
        metaCell.addElement(createMetaLine("N° Facture : ", invoice.getInvoiceNumber()));
        metaCell.addElement(createMetaLine("Date création : ", formatDate(invoice.getCreatedAt())));
        if (invoice.getIssuedAt() != null) {
            metaCell.addElement(createMetaLine("Date émission : ", formatDate(invoice.getIssuedAt())));
        }
        if (invoice.getPaidAt() != null) {
            metaCell.addElement(createMetaLine("Date règlement : ", formatDate(invoice.getPaidAt())));
        }
        if (invoice.getSaleOrder() != null) {
            metaCell.addElement(createMetaLine("Commande liée : ", invoice.getSaleOrder().getOrderNumber()));
        }
        infoTable.addCell(metaCell);

        // Right box: Client info
        PdfPCell partyCell = new PdfPCell();
        partyCell.setBorder(Rectangle.BOX);
        partyCell.setBorderColor(BORDER_COLOR);
        partyCell.setPadding(8);
        partyCell.addElement(new Paragraph("CLIENT", FONT_SECTION));
        Client client = invoice.getClient();
        partyCell.addElement(new Paragraph(client.getName(), FONT_BOLD));
        if (client.getAddress() != null && !client.getAddress().isBlank()) {
            partyCell.addElement(new Paragraph(client.getAddress(), FONT_REGULAR));
        }
        if (client.getCity() != null && !client.getCity().isBlank()) {
            partyCell.addElement(new Paragraph(client.getCity(), FONT_REGULAR));
        }
        if (client.getPhone() != null && !client.getPhone().isBlank()) {
            partyCell.addElement(createMetaLine("Tél : ", client.getPhone()));
        }
        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            partyCell.addElement(createMetaLine("Email : ", client.getEmail()));
        }
        if (client.getTaxNumber() != null && !client.getTaxNumber().isBlank()) {
            partyCell.addElement(createMetaLine("Identifiant Fiscal : ", client.getTaxNumber()));
        }
        infoTable.addCell(partyCell);

        document.add(infoTable);

        // 3. Line Items Table
        PdfPTable itemsTable = createItemsTable();
        List<CustomerInvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        boolean alt = false;
        for (CustomerInvoiceItem item : items) {
            String ref = item.getArticle() != null ? item.getArticle().getReference() : "—";
            String des = item.getArticle() != null ? item.getArticle().getDesignation() : "—";
            String unit = item.getArticle() != null && item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : "";
            addItemRow(itemsTable, ref, des, unit, item.getQuantity(), item.getUnitPrice(), item.getTaxRate(), item.getTotalHt(), item.getTotalTtc(), alt);
            alt = !alt;
        }
        document.add(itemsTable);

        // 4. Totals Summary Table
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(45);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);

        addTotalRow(totalsTable, "Sous-total HT :", formatMoney(invoice.getSubtotalHt()), false);
        String taxLabel = String.format("TVA (%s%%) :", formatPercentage(invoice.getTaxRate()));
        addTotalRow(totalsTable, taxLabel, formatMoney(invoice.getTaxAmount()), false);
        addTotalRow(totalsTable, "TOTAL TTC :", formatMoney(invoice.getTotalTtc()), true);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Montant déjà réglé :", formatMoney(paidAmount), false);
            addTotalRow(totalsTable, "Reste à payer :", formatMoney(balanceDue), true);
        }
        document.add(totalsTable);

        // 5. Notes if present
        addNotes(document, invoice.getNotes());

        document.close();
        String filename = "Facture_" + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : id) + ".pdf";
        return new PdfDocument(baos.toByteArray(), filename);
    }

    @Transactional(readOnly = true)
    public PdfDocument generateSupplierInvoicePdf(Long id) {
        SupplierInvoice invoice = supplierInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture fournisseur introuvable: ID " + id));

        BigDecimal paidAmount = paymentRepository.sumAmountBySupplierInvoiceId(id);
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        BigDecimal totalTtc = invoice.getTotalTtc() != null ? invoice.getTotalTtc() : BigDecimal.ZERO;
        BigDecimal balanceDue = totalTtc.subtract(paidAmount).max(BigDecimal.ZERO);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 45);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageNumberEvent());
        document.open();

        addHeader(document, "FACTURE FOURNISSEUR", invoice.getInvoiceNumber(), invoice.getStatus().name());

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.BOX);
        metaCell.setBorderColor(BORDER_COLOR);
        metaCell.setPadding(8);
        metaCell.setBackgroundColor(ALT_ROW_BG);
        metaCell.addElement(new Paragraph("DÉTAILS DU DOCUMENT", FONT_SECTION));
        metaCell.addElement(createMetaLine("N° Facture : ", invoice.getInvoiceNumber()));
        metaCell.addElement(createMetaLine("Date création : ", formatDate(invoice.getCreatedAt())));
        if (invoice.getReceivedAt() != null) {
            metaCell.addElement(createMetaLine("Date réception : ", formatDate(invoice.getReceivedAt())));
        }
        if (invoice.getPaidAt() != null) {
            metaCell.addElement(createMetaLine("Date règlement : ", formatDate(invoice.getPaidAt())));
        }
        if (invoice.getPurchaseOrder() != null) {
            metaCell.addElement(createMetaLine("Commande liée : ", invoice.getPurchaseOrder().getOrderNumber()));
        }
        infoTable.addCell(metaCell);

        PdfPCell partyCell = new PdfPCell();
        partyCell.setBorder(Rectangle.BOX);
        partyCell.setBorderColor(BORDER_COLOR);
        partyCell.setPadding(8);
        partyCell.addElement(new Paragraph("FOURNISSEUR", FONT_SECTION));
        Supplier supplier = invoice.getSupplier();
        partyCell.addElement(new Paragraph(supplier.getName(), FONT_BOLD));
        if (supplier.getAddress() != null && !supplier.getAddress().isBlank()) {
            partyCell.addElement(new Paragraph(supplier.getAddress(), FONT_REGULAR));
        }
        if (supplier.getPhone() != null && !supplier.getPhone().isBlank()) {
            partyCell.addElement(createMetaLine("Tél : ", supplier.getPhone()));
        }
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()) {
            partyCell.addElement(createMetaLine("Email : ", supplier.getEmail()));
        }
        infoTable.addCell(partyCell);

        document.add(infoTable);

        PdfPTable itemsTable = createItemsTable();
        List<SupplierInvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        boolean alt = false;
        for (SupplierInvoiceItem item : items) {
            String ref = item.getArticle() != null ? item.getArticle().getReference() : "—";
            String des = item.getArticle() != null ? item.getArticle().getDesignation() : "—";
            String unit = item.getArticle() != null && item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : "";
            addItemRow(itemsTable, ref, des, unit, item.getQuantity(), item.getUnitPrice(), item.getTaxRate(), item.getTotalHt(), item.getTotalTtc(), alt);
            alt = !alt;
        }
        document.add(itemsTable);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(45);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);

        addTotalRow(totalsTable, "Sous-total HT :", formatMoney(invoice.getSubtotalHt()), false);
        String taxLabel = String.format("TVA (%s%%) :", formatPercentage(invoice.getTaxRate()));
        addTotalRow(totalsTable, taxLabel, formatMoney(invoice.getTaxAmount()), false);
        addTotalRow(totalsTable, "TOTAL TTC :", formatMoney(invoice.getTotalTtc()), true);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Montant déjà réglé :", formatMoney(paidAmount), false);
            addTotalRow(totalsTable, "Reste à payer :", formatMoney(balanceDue), true);
        }
        document.add(totalsTable);

        addNotes(document, invoice.getNotes());

        document.close();
        String filename = "Facture_Fournisseur_" + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : id) + ".pdf";
        return new PdfDocument(baos.toByteArray(), filename);
    }

    @Transactional(readOnly = true)
    public PdfDocument generateSaleOrderPdf(Long id) {
        SaleOrder order = saleOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande client introuvable: ID " + id));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 45);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageNumberEvent());
        document.open();

        addHeader(document, "BON DE COMMANDE CLIENT", order.getOrderNumber(), order.getStatus().name());

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.BOX);
        metaCell.setBorderColor(BORDER_COLOR);
        metaCell.setPadding(8);
        metaCell.setBackgroundColor(ALT_ROW_BG);
        metaCell.addElement(new Paragraph("DÉTAILS DE LA COMMANDE", FONT_SECTION));
        metaCell.addElement(createMetaLine("N° Commande : ", order.getOrderNumber()));
        metaCell.addElement(createMetaLine("Date création : ", formatDate(order.getCreatedAt())));
        if (order.getConfirmedAt() != null) {
            metaCell.addElement(createMetaLine("Date confirmation : ", formatDate(order.getConfirmedAt())));
        }
        if (order.getDeliveredAt() != null) {
            metaCell.addElement(createMetaLine("Date livraison : ", formatDate(order.getDeliveredAt())));
        }
        if (order.getWarehouse() != null) {
            String wh = order.getWarehouse().getName() + " (" + order.getWarehouse().getCode() + ")";
            metaCell.addElement(createMetaLine("Entrepôt : ", wh));
        }
        infoTable.addCell(metaCell);

        PdfPCell partyCell = new PdfPCell();
        partyCell.setBorder(Rectangle.BOX);
        partyCell.setBorderColor(BORDER_COLOR);
        partyCell.setPadding(8);
        partyCell.addElement(new Paragraph("CLIENT", FONT_SECTION));
        Client client = order.getClient();
        partyCell.addElement(new Paragraph(client.getName(), FONT_BOLD));
        if (client.getAddress() != null && !client.getAddress().isBlank()) {
            partyCell.addElement(new Paragraph(client.getAddress(), FONT_REGULAR));
        }
        if (client.getCity() != null && !client.getCity().isBlank()) {
            partyCell.addElement(new Paragraph(client.getCity(), FONT_REGULAR));
        }
        if (client.getPhone() != null && !client.getPhone().isBlank()) {
            partyCell.addElement(createMetaLine("Tél : ", client.getPhone()));
        }
        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            partyCell.addElement(createMetaLine("Email : ", client.getEmail()));
        }
        if (client.getTaxNumber() != null && !client.getTaxNumber().isBlank()) {
            partyCell.addElement(createMetaLine("Identifiant Fiscal : ", client.getTaxNumber()));
        }
        infoTable.addCell(partyCell);

        document.add(infoTable);

        PdfPTable itemsTable = createItemsTable();
        List<SaleOrderItem> items = order.getItems() != null ? order.getItems() : List.of();
        boolean alt = false;
        for (SaleOrderItem item : items) {
            String ref = item.getArticle() != null ? item.getArticle().getReference() : "—";
            String des = item.getArticle() != null ? item.getArticle().getDesignation() : "—";
            String unit = item.getArticle() != null && item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : "";
            addItemRow(itemsTable, ref, des, unit, item.getQuantity(), item.getUnitPrice(), item.getTaxRate(), item.getTotalHt(), item.getTotalTtc(), alt);
            alt = !alt;
        }
        document.add(itemsTable);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(45);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);

        addTotalRow(totalsTable, "Sous-total HT :", formatMoney(order.getSubtotalHt()), false);
        String taxLabel = String.format("TVA (%s%%) :", formatPercentage(order.getTaxRate()));
        addTotalRow(totalsTable, taxLabel, formatMoney(order.getTaxAmount()), false);
        addTotalRow(totalsTable, "TOTAL TTC :", formatMoney(order.getTotalTtc()), true);
        document.add(totalsTable);

        addNotes(document, order.getNotes());

        document.close();
        String filename = "Commande_Client_" + (order.getOrderNumber() != null ? order.getOrderNumber() : id) + ".pdf";
        return new PdfDocument(baos.toByteArray(), filename);
    }

    @Transactional(readOnly = true)
    public PdfDocument generatePurchaseOrderPdf(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bon de commande fournisseur introuvable: ID " + id));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 45);
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        writer.setPageEvent(new PdfPageNumberEvent());
        document.open();

        addHeader(document, "BON DE COMMANDE FOURNISSEUR", order.getOrderNumber(), order.getStatus().name());

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.BOX);
        metaCell.setBorderColor(BORDER_COLOR);
        metaCell.setPadding(8);
        metaCell.setBackgroundColor(ALT_ROW_BG);
        metaCell.addElement(new Paragraph("DÉTAILS DE LA COMMANDE", FONT_SECTION));
        metaCell.addElement(createMetaLine("N° Bon de commande : ", order.getOrderNumber()));
        metaCell.addElement(createMetaLine("Date création : ", formatDate(order.getCreatedAt())));
        if (order.getConfirmedAt() != null) {
            metaCell.addElement(createMetaLine("Date confirmation : ", formatDate(order.getConfirmedAt())));
        }
        if (order.getReceivedAt() != null) {
            metaCell.addElement(createMetaLine("Date réception : ", formatDate(order.getReceivedAt())));
        }
        if (order.getWarehouse() != null) {
            String wh = order.getWarehouse().getName() + " (" + order.getWarehouse().getCode() + ")";
            metaCell.addElement(createMetaLine("Entrepôt destinataire : ", wh));
        }
        infoTable.addCell(metaCell);

        PdfPCell partyCell = new PdfPCell();
        partyCell.setBorder(Rectangle.BOX);
        partyCell.setBorderColor(BORDER_COLOR);
        partyCell.setPadding(8);
        partyCell.addElement(new Paragraph("FOURNISSEUR", FONT_SECTION));
        Supplier supplier = order.getSupplier();
        partyCell.addElement(new Paragraph(supplier.getName(), FONT_BOLD));
        if (supplier.getAddress() != null && !supplier.getAddress().isBlank()) {
            partyCell.addElement(new Paragraph(supplier.getAddress(), FONT_REGULAR));
        }
        if (supplier.getPhone() != null && !supplier.getPhone().isBlank()) {
            partyCell.addElement(createMetaLine("Tél : ", supplier.getPhone()));
        }
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()) {
            partyCell.addElement(createMetaLine("Email : ", supplier.getEmail()));
        }
        infoTable.addCell(partyCell);

        document.add(infoTable);

        PdfPTable itemsTable = createItemsTable();
        List<PurchaseOrderItem> items = order.getItems() != null ? order.getItems() : List.of();
        boolean alt = false;
        for (PurchaseOrderItem item : items) {
            String ref = item.getArticle() != null ? item.getArticle().getReference() : "—";
            String des = item.getArticle() != null ? item.getArticle().getDesignation() : "—";
            String unit = item.getArticle() != null && item.getArticle().getUnit() != null ? item.getArticle().getUnit().getSymbol() : "";
            addItemRow(itemsTable, ref, des, unit, item.getQuantity(), item.getUnitPrice(), item.getTaxRate(), item.getTotalHt(), item.getTotalTtc(), alt);
            alt = !alt;
        }
        document.add(itemsTable);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(45);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingBefore(10);

        addTotalRow(totalsTable, "Sous-total HT :", formatMoney(order.getSubtotalHt()), false);
        String taxLabel = String.format("TVA (%s%%) :", formatPercentage(order.getTaxRate()));
        addTotalRow(totalsTable, taxLabel, formatMoney(order.getTaxAmount()), false);
        addTotalRow(totalsTable, "TOTAL TTC :", formatMoney(order.getTotalTtc()), true);
        document.add(totalsTable);

        addNotes(document, order.getNotes());

        document.close();
        String filename = "Bon_Commande_" + (order.getOrderNumber() != null ? order.getOrderNumber() : id) + ".pdf";
        return new PdfDocument(baos.toByteArray(), filename);
    }

    private void addHeader(Document document, String documentTypeTitle, String documentNumber, String status) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{55, 45});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("NovaERP", FONT_TITLE));
        left.addElement(new Paragraph("Système Intégré de Gestion Commerciale & Stocks", FONT_MUTED));
        headerTable.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph typeP = new Paragraph(documentTypeTitle, FONT_SUBTITLE);
        typeP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(typeP);

        Paragraph numP = new Paragraph("N° " + documentNumber, FONT_BOLD);
        numP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(numP);

        Paragraph statusP = new Paragraph("Statut : " + formatStatus(status), FONT_MUTED_BOLD);
        statusP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(statusP);

        headerTable.addCell(right);
        document.add(headerTable);
    }

    private PdfPTable createItemsTable() throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setWidths(new float[]{14, 30, 8, 8, 13, 9, 13, 15});
        table.setSpacingBefore(10);

        String[] headers = {"RÉF.", "DÉSIGNATION", "UNITÉ", "QTÉ", "P.U. HT", "TVA", "TOTAL HT", "TOTAL TTC"};
        int[] aligns = {Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};

        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], FONT_TABLE_HEADER));
            cell.setBackgroundColor(HEADER_BG);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(BORDER_COLOR);
            cell.setBorderWidth(1.2f);
            cell.setPadding(6);
            cell.setHorizontalAlignment(aligns[i]);
            table.addCell(cell);
        }
        return table;
    }

    private void addItemRow(PdfPTable table, String ref, String designation, String unit, BigDecimal qty,
                            BigDecimal unitPrice, BigDecimal taxRate, BigDecimal totalHt, BigDecimal totalTtc, boolean alt) {
        Color bg = alt ? ALT_ROW_BG : Color.WHITE;

        addTableCell(table, ref, FONT_TABLE_ROW, Element.ALIGN_LEFT, bg);
        addTableCell(table, designation, FONT_TABLE_ROW, Element.ALIGN_LEFT, bg);
        addTableCell(table, unit != null ? unit : "—", FONT_TABLE_ROW, Element.ALIGN_CENTER, bg);
        addTableCell(table, formatQty(qty), FONT_TABLE_ROW, Element.ALIGN_RIGHT, bg);
        addTableCell(table, formatMoneyOnly(unitPrice), FONT_TABLE_ROW, Element.ALIGN_RIGHT, bg);
        addTableCell(table, formatPercentage(taxRate) + "%", FONT_TABLE_ROW, Element.ALIGN_RIGHT, bg);
        addTableCell(table, formatMoneyOnly(totalHt), FONT_TABLE_ROW, Element.ALIGN_RIGHT, bg);
        addTableCell(table, formatMoney(totalTtc), FONT_TABLE_ROW_BOLD, Element.ALIGN_RIGHT, bg);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(0.5f);
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isHighlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, isHighlight ? FONT_TOTAL_MAIN_TITLE : FONT_TOTAL_TITLE));
        labelCell.setBorder(isHighlight ? Rectangle.TOP : Rectangle.NO_BORDER);
        labelCell.setBorderColor(BORDER_COLOR);
        labelCell.setPadding(4);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell valCell = new PdfPCell(new Phrase(value, isHighlight ? FONT_TOTAL_MAIN_VAL : FONT_TOTAL_VAL));
        valCell.setBorder(isHighlight ? Rectangle.TOP : Rectangle.NO_BORDER);
        valCell.setBorderColor(BORDER_COLOR);
        valCell.setPadding(4);
        valCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valCell);
    }

    private void addNotes(Document document, String notes) throws DocumentException {
        if (notes != null && !notes.isBlank()) {
            Paragraph p = new Paragraph();
            p.setSpacingBefore(12);
            p.add(new Phrase("Notes / Observations : ", FONT_BOLD));
            p.add(new Phrase(notes, FONT_REGULAR));
            document.add(p);
        }
    }

    private Paragraph createMetaLine(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(label, FONT_MUTED_BOLD));
        p.add(new Phrase(value, FONT_REGULAR));
        return p;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0,00 MAD";
        return MONEY_FORMAT.format(amount) + " MAD";
    }

    private String formatMoneyOnly(BigDecimal amount) {
        if (amount == null) return "0,00";
        return MONEY_FORMAT.format(amount);
    }

    private String formatQty(BigDecimal qty) {
        if (qty == null) return "0";
        if (qty.stripTrailingZeros().scale() <= 0) {
            return qty.toBigInteger().toString();
        }
        return qty.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private String formatPercentage(BigDecimal rate) {
        if (rate == null) return "0";
        return rate.stripTrailingZeros().toPlainString();
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "—";
        return DATE_FORMATTER.format(instant);
    }

    private String formatStatus(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "DRAFT" -> "BROUILLON";
            case "CONFIRMED" -> "CONFIRMÉE";
            case "ISSUED" -> "ÉMISE";
            case "RECEIVED" -> "REÇUE";
            case "DELIVERED" -> "LIVRÉE";
            case "PAID" -> "PAYÉE";
            case "CANCELLED" -> "ANNULÉE";
            default -> status;
        };
    }

    private static class PdfPageNumberEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new int[]{60, 40});
                footer.setTotalWidth(document.right() - document.left());
                footer.getDefaultCell().setBorder(Rectangle.NO_BORDER);

                PdfPCell left = new PdfPCell(new Phrase("NovaERP — Document édité électroniquement", FONT_MUTED));
                left.setBorder(Rectangle.TOP);
                left.setBorderColor(BORDER_COLOR);
                left.setPaddingTop(6);

                PdfPCell right = new PdfPCell(new Phrase("Page " + writer.getPageNumber(), FONT_MUTED));
                right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                right.setBorder(Rectangle.TOP);
                right.setBorderColor(BORDER_COLOR);
                right.setPaddingTop(6);

                footer.addCell(left);
                footer.addCell(right);
                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, writer.getDirectContent());
            } catch (Exception ignored) {
            }
        }
    }
}
