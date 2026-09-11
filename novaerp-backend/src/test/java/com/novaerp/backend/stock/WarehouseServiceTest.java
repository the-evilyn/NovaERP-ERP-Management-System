package com.novaerp.backend.stock;

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
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseLocationRepository warehouseLocationRepository;

    @Mock
    private WarehouseStockRepository warehouseStockRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse sampleWarehouse;
    private WarehouseLocation sampleLocation;

    @BeforeEach
    void setUp() {
        sampleWarehouse = Warehouse.builder()
                .id(1L)
                .code("WH-MAIN")
                .name("Entrepôt Principal")
                .description("Dépôt central")
                .address("Casablanca")
                .active(true)
                .isDefault(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleLocation = WarehouseLocation.builder()
                .id(10L)
                .warehouse(sampleWarehouse)
                .code("LOC-GEN")
                .name("Zone Générale")
                .description("Zone par défaut")
                .active(true)
                .isDefault(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ==========================================
    // Warehouse Tests
    // ==========================================

    @Test
    @DisplayName("createWarehouse: successfully creates warehouse when code is unique")
    void testCreateWarehouse_Success() {
        WarehouseRequest req = new WarehouseRequest("WH-TNG", "Entrepôt Tanger", "Zone Franche", "Tanger");
        when(warehouseRepository.existsByCode("WH-TNG")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            w.setId(2L);
            return w;
        });

        WarehouseResponse res = warehouseService.createWarehouse(req);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(2L);
        assertThat(res.code()).isEqualTo("WH-TNG");
        assertThat(res.name()).isEqualTo("Entrepôt Tanger");
        assertThat(res.active()).isTrue();
        assertThat(res.isDefault()).isFalse();
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    @DisplayName("createWarehouse: rejects duplicate code with 409 CONFLICT")
    void testCreateWarehouse_DuplicateCode_ThrowsConflict() {
        WarehouseRequest req = new WarehouseRequest("WH-MAIN", "Duplicate", null, null);
        when(warehouseRepository.existsByCode("WH-MAIN")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateWarehouse: successfully updates warehouse fields when code is unique")
    void testUpdateWarehouse_Success() {
        WarehouseRequest req = new WarehouseRequest("WH-MAIN-UPDATED", "Entrepôt Rénové", "Nouvelle desc", "Nouvelle adresse");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.existsByCodeAndIdNot("WH-MAIN-UPDATED", 1L)).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(sampleWarehouse);

        WarehouseResponse res = warehouseService.updateWarehouse(1L, req);

        assertThat(res.code()).isEqualTo("WH-MAIN-UPDATED");
        assertThat(res.name()).isEqualTo("Entrepôt Rénové");
        verify(warehouseRepository).save(sampleWarehouse);
    }

    @Test
    @DisplayName("updateWarehouse: rejects duplicate code on update with 409 CONFLICT")
    void testUpdateWarehouse_DuplicateCode_ThrowsConflict() {
        WarehouseRequest req = new WarehouseRequest("WH-OTHER", "Name", null, null);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.existsByCodeAndIdNot("WH-OTHER", 1L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.updateWarehouse(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateWarehouse: throws 404 NOT_FOUND when warehouse does not exist")
    void testUpdateWarehouse_NotFound_ThrowsNotFound() {
        WarehouseRequest req = new WarehouseRequest("WH-XYZ", "Name", null, null);
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateWarehouse(99L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("getWarehouse: returns warehouse response or throws 404")
    void testGetWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        WarehouseResponse res = warehouseService.getWarehouse(1L);
        assertThat(res.code()).isEqualTo("WH-MAIN");

        assertThatThrownBy(() -> warehouseService.getWarehouse(99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("listWarehouses: supports unfiltered and active-filtered pagination")
    void testListWarehouses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Warehouse> page = new PageImpl<>(List.of(sampleWarehouse), pageable, 1);

        when(warehouseRepository.findAll(pageable)).thenReturn(page);
        Page<WarehouseResponse> all = warehouseService.listWarehouses(null, pageable);
        assertThat(all.getTotalElements()).isEqualTo(1);

        when(warehouseRepository.findByActive(true, pageable)).thenReturn(page);
        Page<WarehouseResponse> active = warehouseService.listWarehouses(true, pageable);
        assertThat(active.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("setActiveWarehouse: cannot deactivate default warehouse")
    void testDeactivateWarehouse_DefaultWarehouse_ThrowsBadRequest() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));

        assertThatThrownBy(() -> warehouseService.setActiveWarehouse(1L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("default");
                });

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveWarehouse: cannot deactivate warehouse containing positive stock")
    void testDeactivateWarehouse_PositiveStock_ThrowsBadRequest() {
        Warehouse nonDefaultWh = Warehouse.builder()
                .id(2L)
                .code("WH-NON-DEFAULT")
                .name("Entrepôt Secondaire")
                .active(true)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(nonDefaultWh));
        when(warehouseStockRepository.countByWarehouseIdAndQuantityGreaterThan(2L, BigDecimal.ZERO)).thenReturn(5L);

        assertThatThrownBy(() -> warehouseService.setActiveWarehouse(2L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("stock");
                });

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveWarehouse: can deactivate warehouse with zero stock")
    void testDeactivateWarehouse_ZeroStock_Success() {
        Warehouse nonDefaultWh = Warehouse.builder()
                .id(2L)
                .code("WH-EMPTY")
                .name("Entrepôt Vide")
                .active(true)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(nonDefaultWh));
        when(warehouseStockRepository.countByWarehouseIdAndQuantityGreaterThan(2L, BigDecimal.ZERO)).thenReturn(0L);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseResponse res = warehouseService.setActiveWarehouse(2L, false);

        assertThat(res.active()).isFalse();
        verify(warehouseRepository).save(nonDefaultWh);
    }

    @Test
    @DisplayName("setActiveWarehouse: can activate an inactive warehouse")
    void testActivateWarehouse_Success() {
        Warehouse inactiveWh = Warehouse.builder()
                .id(3L)
                .code("WH-INACTIVE")
                .name("Entrepôt Inactif")
                .active(false)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(3L)).thenReturn(Optional.of(inactiveWh));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseResponse res = warehouseService.setActiveWarehouse(3L, true);

        assertThat(res.active()).isTrue();
        verify(warehouseRepository).save(inactiveWh);
        verify(warehouseStockRepository, never()).countByWarehouseIdAndQuantityGreaterThan(any(), any());
    }

    @Test
    @DisplayName("Service design verification: WarehouseService exposes no physical delete methods")
    void testCannotPhysicallyDelete() {
        List<String> methodNames = Arrays.stream(WarehouseService.class.getDeclaredMethods())
                .map(Method::getName)
                .map(String::toLowerCase)
                .toList();

        assertThat(methodNames).noneMatch(name -> name.contains("delete") || name.contains("remove"));
    }

    // ==========================================
    // Location Tests
    // ==========================================

    @Test
    @DisplayName("createLocation: successfully creates location in active warehouse")
    void testCreateLocation_InActiveWarehouse_Success() {
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-A1", "Allée A Rack 1", "Zone A");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.existsByWarehouseIdAndCode(1L, "LOC-A1")).thenReturn(false);
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenAnswer(inv -> {
            WarehouseLocation loc = inv.getArgument(0);
            loc.setId(20L);
            return loc;
        });

        WarehouseLocationResponse res = warehouseService.createLocation(1L, req);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(20L);
        assertThat(res.warehouseId()).isEqualTo(1L);
        assertThat(res.warehouseCode()).isEqualTo("WH-MAIN");
        assertThat(res.code()).isEqualTo("LOC-A1");
        assertThat(res.active()).isTrue();
        assertThat(res.isDefault()).isFalse();
        verify(warehouseLocationRepository).save(any(WarehouseLocation.class));
    }

    @Test
    @DisplayName("createLocation: rejects creation in nonexistent warehouse with 404")
    void testCreateLocation_NonExistentWarehouse_ThrowsNotFound() {
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-A1", "Allée A", null);
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.createLocation(99L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLocation: rejects creation in inactive warehouse with 400 BAD_REQUEST")
    void testCreateLocation_InactiveWarehouse_ThrowsBadRequest() {
        Warehouse inactiveWh = Warehouse.builder().id(2L).active(false).build();
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-A1", "Allée A", null);
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(inactiveWh));

        assertThatThrownBy(() -> warehouseService.createLocation(2L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("inactive");
                });

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLocation: rejects duplicate code inside same warehouse with 409 CONFLICT")
    void testCreateLocation_DuplicateCodeInSameWarehouse_ThrowsConflict() {
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-GEN", "Duplicate", null);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.existsByWarehouseIdAndCode(1L, "LOC-GEN")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createLocation(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createLocation: allows same location code in different warehouse")
    void testCreateLocation_SameCodeDifferentWarehouse_Allowed() {
        Warehouse wh2 = Warehouse.builder().id(2L).code("WH-NORTH").active(true).build();
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-GEN", "Zone Générale Nord", null);

        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(wh2));
        when(warehouseLocationRepository.existsByWarehouseIdAndCode(2L, "LOC-GEN")).thenReturn(false);
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenAnswer(inv -> {
            WarehouseLocation loc = inv.getArgument(0);
            loc.setId(30L);
            return loc;
        });

        WarehouseLocationResponse res = warehouseService.createLocation(2L, req);

        assertThat(res.id()).isEqualTo(30L);
        assertThat(res.warehouseId()).isEqualTo(2L);
        assertThat(res.code()).isEqualTo("LOC-GEN");
        verify(warehouseLocationRepository).save(any(WarehouseLocation.class));
    }

    @Test
    @DisplayName("updateLocation: successfully updates location when ownership and code uniqueness pass")
    void testUpdateLocation_Success() {
        WarehouseLocation nonDefaultLoc = WarehouseLocation.builder()
                .id(11L)
                .warehouse(sampleWarehouse)
                .code("LOC-A1")
                .name("Old Name")
                .active(true)
                .build();

        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-A1-NEW", "New Name", "New Desc");
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseLocationRepository.findByIdAndWarehouseId(11L, 1L)).thenReturn(Optional.of(nonDefaultLoc));
        when(warehouseLocationRepository.existsByWarehouseIdAndCodeAndIdNot(1L, "LOC-A1-NEW", 11L)).thenReturn(false);
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenReturn(nonDefaultLoc);

        WarehouseLocationResponse res = warehouseService.updateLocation(1L, 11L, req);

        assertThat(res.code()).isEqualTo("LOC-A1-NEW");
        assertThat(res.name()).isEqualTo("New Name");
        verify(warehouseLocationRepository).save(nonDefaultLoc);
    }

    @Test
    @DisplayName("updateLocation: validates ownership and throws 404 when location does not belong to warehouse")
    void testUpdateLocation_OwnershipValidation_ThrowsNotFound() {
        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-A1", "Name", null);
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseLocationRepository.findByIdAndWarehouseId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateLocation(1L, 99L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateLocation: rejects duplicate location code inside same warehouse")
    void testUpdateLocation_DuplicateCode_ThrowsConflict() {
        WarehouseLocation nonDefaultLoc = WarehouseLocation.builder()
                .id(11L)
                .warehouse(sampleWarehouse)
                .code("LOC-A1")
                .name("Old Name")
                .build();

        WarehouseLocationRequest req = new WarehouseLocationRequest("LOC-GEN", "New Name", null);
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseLocationRepository.findByIdAndWarehouseId(11L, 1L)).thenReturn(Optional.of(nonDefaultLoc));
        when(warehouseLocationRepository.existsByWarehouseIdAndCodeAndIdNot(1L, "LOC-GEN", 11L)).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.updateLocation(1L, 11L, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("listLocations: retrieves paginated locations scoped to warehouse")
    void testListLocations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<WarehouseLocation> page = new PageImpl<>(List.of(sampleLocation), pageable, 1);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseLocationRepository.findByWarehouseId(1L, pageable)).thenReturn(page);
        Page<WarehouseLocationResponse> all = warehouseService.listLocations(1L, null, pageable);
        assertThat(all.getTotalElements()).isEqualTo(1);

        when(warehouseLocationRepository.findByWarehouseIdAndActive(1L, true, pageable)).thenReturn(page);
        Page<WarehouseLocationResponse> active = warehouseService.listLocations(1L, true, pageable);
        assertThat(active.getTotalElements()).isEqualTo(1);

        when(warehouseRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> warehouseService.listLocations(99L, null, pageable))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("getLocation: returns location scoped to warehouse or throws 404")
    void testGetLocation() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseLocationRepository.findByIdAndWarehouseId(10L, 1L)).thenReturn(Optional.of(sampleLocation));
        when(warehouseLocationRepository.findByIdAndWarehouseId(99L, 1L)).thenReturn(Optional.empty());

        WarehouseLocationResponse res = warehouseService.getLocation(1L, 10L);
        assertThat(res.code()).isEqualTo("LOC-GEN");

        assertThatThrownBy(() -> warehouseService.getLocation(1L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("setActiveLocation: cannot deactivate default location")
    void testDeactivateLocation_DefaultLocation_ThrowsBadRequest() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.findByIdAndWarehouseId(10L, 1L)).thenReturn(Optional.of(sampleLocation));

        assertThatThrownBy(() -> warehouseService.setActiveLocation(1L, 10L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("default");
                });

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveLocation: cannot deactivate location containing positive stock")
    void testDeactivateLocation_PositiveStock_ThrowsBadRequest() {
        WarehouseLocation nonDefaultLoc = WarehouseLocation.builder()
                .id(11L)
                .warehouse(sampleWarehouse)
                .code("LOC-A1")
                .active(true)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.findByIdAndWarehouseId(11L, 1L)).thenReturn(Optional.of(nonDefaultLoc));
        when(warehouseStockRepository.countByLocationIdAndQuantityGreaterThan(11L, BigDecimal.ZERO)).thenReturn(3L);

        assertThatThrownBy(() -> warehouseService.setActiveLocation(1L, 11L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("stock");
                });

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveLocation: can deactivate location with zero stock")
    void testDeactivateLocation_ZeroStock_Success() {
        WarehouseLocation nonDefaultLoc = WarehouseLocation.builder()
                .id(11L)
                .warehouse(sampleWarehouse)
                .code("LOC-A1")
                .active(true)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.findByIdAndWarehouseId(11L, 1L)).thenReturn(Optional.of(nonDefaultLoc));
        when(warehouseStockRepository.countByLocationIdAndQuantityGreaterThan(11L, BigDecimal.ZERO)).thenReturn(0L);
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseLocationResponse res = warehouseService.setActiveLocation(1L, 11L, false);

        assertThat(res.active()).isFalse();
        verify(warehouseLocationRepository).save(nonDefaultLoc);
    }

    @Test
    @DisplayName("setActiveLocation: cannot activate location under inactive warehouse")
    void testActivateLocation_InactiveWarehouse_ThrowsBadRequest() {
        Warehouse inactiveWh = Warehouse.builder().id(2L).active(false).build();
        WarehouseLocation inactiveLoc = WarehouseLocation.builder()
                .id(12L)
                .warehouse(inactiveWh)
                .code("LOC-B1")
                .active(false)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(inactiveWh));
        when(warehouseLocationRepository.findByIdAndWarehouseId(12L, 2L)).thenReturn(Optional.of(inactiveLoc));

        assertThatThrownBy(() -> warehouseService.setActiveLocation(2L, 12L, true))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("inactive");
                });

        verify(warehouseLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveLocation: successfully activates location under active warehouse")
    void testActivateLocation_ActiveWarehouse_Success() {
        WarehouseLocation inactiveLoc = WarehouseLocation.builder()
                .id(12L)
                .warehouse(sampleWarehouse)
                .code("LOC-B1")
                .active(false)
                .isDefault(false)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sampleWarehouse));
        when(warehouseLocationRepository.findByIdAndWarehouseId(12L, 1L)).thenReturn(Optional.of(inactiveLoc));
        when(warehouseLocationRepository.save(any(WarehouseLocation.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseLocationResponse res = warehouseService.setActiveLocation(1L, 12L, true);

        assertThat(res.active()).isTrue();
        verify(warehouseLocationRepository).save(inactiveLoc);
        verify(warehouseStockRepository, never()).countByLocationIdAndQuantityGreaterThan(any(), any());
    }
}
