package com.novaerp.backend.stock;

import com.novaerp.backend.stock.dto.ImportResultResponse;
import com.novaerp.backend.stock.dto.SupplierRequest;
import com.novaerp.backend.stock.dto.SupplierResponse;
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
class SupplierControllerTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private StockImportExportService importExportService;

    @InjectMocks
    private SupplierController supplierController;

    private Supplier sampleSupplier;

    @BeforeEach
    void setUp() {
        sampleSupplier = Supplier.builder()
                .id(1L)
                .name("Industrie Marocaine SARL")
                .email("contact@indmaroc.ma")
                .phone("+212 522 123456")
                .address("Zone Industrielle Ain Sebaa, Casablanca")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("list() returns paginated suppliers")
    void testList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Supplier> page = new PageImpl<>(List.of(sampleSupplier), pageable, 1);
        when(supplierRepository.findAll(pageable)).thenReturn(page);

        Page<SupplierResponse> res = supplierController.list(pageable);

        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).name()).isEqualTo("Industrie Marocaine SARL");
        verify(supplierRepository).findAll(pageable);
    }

    @Test
    @DisplayName("get(id) returns supplier when found")
    void testGet_Found() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        SupplierResponse res = supplierController.get(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.name()).isEqualTo("Industrie Marocaine SARL");
    }

    @Test
    @DisplayName("get(id) throws NOT_FOUND when missing")
    void testGet_NotFound() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierController.get(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("create() saves supplier and returns 201")
    void testCreate_Success() {
        SupplierRequest req = new SupplierRequest("Maghreb Pièces", "maghreb@pieces.ma", "+212 537 987654", "Rabat");
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        ResponseEntity<SupplierResponse> res = supplierController.create(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().name()).isEqualTo("Maghreb Pièces");
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    @DisplayName("update() updates supplier fields")
    void testUpdate_Success() {
        SupplierRequest req = new SupplierRequest("Industrie Marocaine Nouvelle", "new@indmaroc.ma", "+212 522 999999", "Casablanca Sud");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(sampleSupplier);

        SupplierResponse res = supplierController.update(1L, req);

        assertThat(res.name()).isEqualTo("Industrie Marocaine Nouvelle");
        assertThat(res.email()).isEqualTo("new@indmaroc.ma");
        verify(supplierRepository).save(sampleSupplier);
    }

    @Test
    @DisplayName("delete() removes supplier and returns 204")
    void testDelete_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(sampleSupplier));

        ResponseEntity<Void> res = supplierController.delete(1L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(supplierRepository).delete(sampleSupplier);
    }

    @Test
    @DisplayName("export() returns CSV attachment")
    void testExport() {
        byte[] csv = "name,email,phone,address\n".getBytes(StandardCharsets.UTF_8);
        when(importExportService.exportSuppliers()).thenReturn(csv);

        ResponseEntity<byte[]> res = supplierController.export();

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(csv);
        assertThat(res.getHeaders().getContentDisposition().getFilename()).isEqualTo("suppliers.csv");
    }

    @Test
    @DisplayName("importCsv() invokes service and returns result")
    void testImportCsv() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "suppliers.csv", "text/csv", "name\n".getBytes());
        ImportResultResponse expected = new ImportResultResponse(2, 1, 0, List.of(), List.of());
        when(importExportService.importSuppliers(file)).thenReturn(expected);

        ImportResultResponse res = supplierController.importCsv(file);

        assertThat(res.created()).isEqualTo(2);
        assertThat(res.skipped()).isEqualTo(1);
    }
}
