package com.novaerp.backend.purchases;

import com.novaerp.backend.common.pdf.DocumentPdfService;
import com.novaerp.backend.purchases.dto.PurchaseOrderItemRequest;
import com.novaerp.backend.purchases.dto.PurchaseOrderRequest;
import com.novaerp.backend.purchases.dto.PurchaseOrderResponse;
import com.novaerp.backend.user.Role;
import com.novaerp.backend.user.User;
import com.novaerp.backend.user.UserRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderControllerTest {

    @Mock
    private PurchaseOrderService purchaseOrderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentPdfService documentPdfService;

    @InjectMocks
    private PurchaseOrderController purchaseOrderController;

    private User sampleUser;
    private Authentication sampleAuth;
    private PurchaseOrderResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("admin@novaerp.local")
                .fullName("Admin")
                .role(Role.ADMIN)
                .build();
        sampleAuth = new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        sampleResponse = new PurchaseOrderResponse(
                100L,
                "PO-2026-0001",
                1L,
                "Fournisseur Test",
                PurchaseOrderStatus.DRAFT,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                null,
                null,
                List.of()
        );
    }

    @Test
    @DisplayName("list() delegates to service and returns page")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PurchaseOrderResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(purchaseOrderService.list(null, null, pageable)).thenReturn(page);

        Page<PurchaseOrderResponse> result = purchaseOrderController.list(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).orderNumber()).isEqualTo("PO-2026-0001");
        verify(purchaseOrderService).list(null, null, pageable);
    }

    @Test
    @DisplayName("getById() returns order details")
    void testGetById() {
        when(purchaseOrderService.getById(100L)).thenReturn(sampleResponse);

        PurchaseOrderResponse result = purchaseOrderController.getById(100L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.orderNumber()).isEqualTo("PO-2026-0001");
        verify(purchaseOrderService).getById(100L);
    }

    @Test
    @DisplayName("create() returns 201 CREATED with created order")
    void testCreate() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("10"),
                new BigDecimal("50.00"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Notes");

        when(purchaseOrderService.create(eq(request), any())).thenReturn(sampleResponse);

        ResponseEntity<PurchaseOrderResponse> response = purchaseOrderController.create(request, sampleAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orderNumber()).isEqualTo("PO-2026-0001");
        verify(purchaseOrderService).create(eq(request), any());
    }

    @Test
    @DisplayName("update() delegates to service")
    void testUpdate() {
        PurchaseOrderItemRequest itemReq = new PurchaseOrderItemRequest(
                10L,
                new BigDecimal("20"),
                new BigDecimal("50.00"),
                new BigDecimal("20.00")
        );
        PurchaseOrderRequest request = new PurchaseOrderRequest(1L, List.of(itemReq), new BigDecimal("20.00"), "Updated");

        when(purchaseOrderService.update(eq(100L), eq(request), any())).thenReturn(sampleResponse);

        PurchaseOrderResponse result = purchaseOrderController.update(100L, request, sampleAuth);

        assertThat(result).isNotNull();
        verify(purchaseOrderService).update(eq(100L), eq(request), any());
    }

    @Test
    @DisplayName("confirm() delegates to service")
    void testConfirm() {
        PurchaseOrderResponse confirmedResponse = new PurchaseOrderResponse(
                100L,
                "PO-2026-0001",
                1L,
                "Fournisseur Test",
                PurchaseOrderStatus.CONFIRMED,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );
        when(purchaseOrderService.confirm(eq(100L), any())).thenReturn(confirmedResponse);

        PurchaseOrderResponse result = purchaseOrderController.confirm(100L, sampleAuth);

        assertThat(result.status()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        verify(purchaseOrderService).confirm(eq(100L), any());
    }

    @Test
    @DisplayName("receive() delegates to service")
    void testReceive() {
        PurchaseOrderResponse receivedResponse = new PurchaseOrderResponse(
                100L,
                "PO-2026-0001",
                1L,
                "Fournisseur Test",
                PurchaseOrderStatus.RECEIVED,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of()
        );
        when(purchaseOrderService.receive(eq(100L), any())).thenReturn(receivedResponse);

        PurchaseOrderResponse result = purchaseOrderController.receive(100L, sampleAuth);

        assertThat(result.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        verify(purchaseOrderService).receive(eq(100L), any());
    }

    @Test
    @DisplayName("cancel() delegates to service")
    void testCancel() {
        PurchaseOrderResponse cancelledResponse = new PurchaseOrderResponse(
                100L,
                "PO-2026-0001",
                1L,
                "Fournisseur Test",
                PurchaseOrderStatus.CANCELLED,
                new BigDecimal("500.0000"),
                new BigDecimal("20.00"),
                new BigDecimal("100.0000"),
                new BigDecimal("600.0000"),
                "Note",
                1L,
                "Admin",
                Instant.now(),
                null,
                null,
                List.of()
        );
        when(purchaseOrderService.cancel(eq(100L), any())).thenReturn(cancelledResponse);

        PurchaseOrderResponse result = purchaseOrderController.cancel(100L, sampleAuth);

        assertThat(result.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);
        verify(purchaseOrderService).cancel(eq(100L), any());
    }

    @Test
    @DisplayName("getPdf() returns PDF byte array with inline Content-Disposition")
    void testGetPdf() {
        byte[] pdfBytes = "%PDF-1.4 sample".getBytes();
        DocumentPdfService.PdfDocument pdfDoc = new DocumentPdfService.PdfDocument(pdfBytes, "Bon_Commande_BC-2026-00001.pdf");
        when(documentPdfService.generatePurchaseOrderPdf(100L)).thenReturn(pdfDoc);

        ResponseEntity<byte[]> response = purchaseOrderController.getPdf(100L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("inline; filename=\"Bon_Commande_BC-2026-00001.pdf\"");
        assertThat(response.getBody()).isEqualTo(pdfBytes);
        verify(documentPdfService).generatePurchaseOrderPdf(100L);
    }
}
