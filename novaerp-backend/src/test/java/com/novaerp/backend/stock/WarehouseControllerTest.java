package com.novaerp.backend.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private WarehouseService warehouseService;

    private WarehouseResponse sampleWarehouseResponse;
    private WarehouseLocationResponse sampleLocationResponse;
    private WarehouseStockResponse sampleStockResponse;

    @BeforeEach
    void setUp() {
        sampleWarehouseResponse = new WarehouseResponse(
                1L,
                "WH-MAIN",
                "Entrepôt Principal",
                "Dépôt central",
                "Casablanca",
                true,
                true,
                Instant.now(),
                Instant.now()
        );

        sampleLocationResponse = new WarehouseLocationResponse(
                10L,
                1L,
                "WH-MAIN",
                "Entrepôt Principal",
                "LOC-GEN",
                "Zone Générale",
                "Zone de stockage par défaut",
                true,
                true,
                Instant.now(),
                Instant.now()
        );

        sampleStockResponse = new WarehouseStockResponse(
                1000L,
                1L,
                "WH-MAIN",
                "Entrepôt Principal",
                10L,
                "LOC-GEN",
                "Zone Générale",
                50L,
                "ART-001",
                "Moteur Électrique",
                "Électronique",
                "Pièce",
                new BigDecimal("20.00"),
                new BigDecimal("5.00"),
                new BigDecimal("150.00"),
                new BigDecimal("3000.00"),
                Instant.now()
        );
    }

    // ==========================================
    // Authentication & Authorization Tests
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to GET /api/warehouses is rejected (401 or 403)")
    void testUnauthenticated_GetWarehouses_Rejected() throws Exception {
        mockMvc.perform(get("/api/warehouses"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to GET /api/warehouses/1/locations is rejected (401 or 403)")
    void testUnauthenticated_GetLocations_Rejected() throws Exception {
        mockMvc.perform(get("/api/warehouses/1/locations"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER can GET /api/warehouses")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CanGetWarehouses() throws Exception {
        when(warehouseService.listWarehouses(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleWarehouseResponse)));

        mockMvc.perform(get("/api/warehouses"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_USER can GET /api/warehouses/{warehouseId}/locations")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CanGetLocations() throws Exception {
        when(warehouseService.listLocations(eq(1L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleLocationResponse)));

        mockMvc.perform(get("/api/warehouses/1/locations"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ROLE_USER cannot POST /api/warehouses (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostWarehouse() throws Exception {
        WarehouseRequest request = new WarehouseRequest("WH-TEST", "Entrepôt Test", null, null);

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot PUT /api/warehouses/{id} (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPutWarehouse() throws Exception {
        WarehouseRequest request = new WarehouseRequest("WH-TEST", "Entrepôt Test", null, null);

        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot PATCH /api/warehouses/{id}/active (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPatchWarehouseActive() throws Exception {
        ActiveToggleRequest request = new ActiveToggleRequest(false);

        mockMvc.perform(patch("/api/warehouses/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot POST /api/warehouses/{warehouseId}/locations (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostLocation() throws Exception {
        WarehouseLocationRequest request = new WarehouseLocationRequest("LOC-TEST", "Zone Test", null);

        mockMvc.perform(post("/api/warehouses/1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot PUT /api/warehouses/{warehouseId}/locations/{locationId} (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPutLocation() throws Exception {
        WarehouseLocationRequest request = new WarehouseLocationRequest("LOC-TEST", "Zone Test", null);

        mockMvc.perform(put("/api/warehouses/1/locations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot PATCH /api/warehouses/{warehouseId}/locations/{locationId}/active (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPatchLocationActive() throws Exception {
        ActiveToggleRequest request = new ActiveToggleRequest(false);

        mockMvc.perform(patch("/api/warehouses/1/locations/10/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // Warehouse Functional & Delegation Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/warehouses: returns paginated warehouses and delegates with active filter")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testListWarehouses_Success() throws Exception {
        Page<WarehouseResponse> page = new PageImpl<>(List.of(sampleWarehouseResponse));
        when(warehouseService.listWarehouses(eq(true), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/warehouses")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("WH-MAIN"))
                .andExpect(jsonPath("$.content[0].name").value("Entrepôt Principal"));

        verify(warehouseService).listWarehouses(eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/warehouses/{id}: returns warehouse details and delegates to service")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testGetWarehouse_Success() throws Exception {
        when(warehouseService.getWarehouse(1L)).thenReturn(sampleWarehouseResponse);

        mockMvc.perform(get("/api/warehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("WH-MAIN"))
                .andExpect(jsonPath("$.name").value("Entrepôt Principal"));

        verify(warehouseService).getWarehouse(1L);
    }

    @Test
    @DisplayName("POST /api/warehouses: ADMIN creates warehouse with 201 CREATED and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testCreateWarehouse_Success() throws Exception {
        WarehouseRequest request = new WarehouseRequest("WH-TNG", "Entrepôt Tanger", "Zone Franche", "Tanger");
        WarehouseResponse createdResponse = new WarehouseResponse(
                2L, "WH-TNG", "Entrepôt Tanger", "Zone Franche", "Tanger", true, false, Instant.now(), Instant.now());
        when(warehouseService.createWarehouse(any(WarehouseRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("WH-TNG"))
                .andExpect(jsonPath("$.name").value("Entrepôt Tanger"));

        verify(warehouseService).createWarehouse(refEq(request));
    }

    @Test
    @DisplayName("PUT /api/warehouses/{id}: ADMIN updates warehouse with 200 OK and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testUpdateWarehouse_Success() throws Exception {
        WarehouseRequest request = new WarehouseRequest("WH-MAIN-UPD", "Entrepôt Rénové", "Desc", "Casa");
        WarehouseResponse updatedResponse = new WarehouseResponse(
                1L, "WH-MAIN-UPD", "Entrepôt Rénové", "Desc", "Casa", true, true, Instant.now(), Instant.now());
        when(warehouseService.updateWarehouse(eq(1L), any(WarehouseRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-MAIN-UPD"))
                .andExpect(jsonPath("$.name").value("Entrepôt Rénové"));

        verify(warehouseService).updateWarehouse(eq(1L), refEq(request));
    }

    @Test
    @DisplayName("PATCH /api/warehouses/{id}/active: ADMIN toggles active status with 200 OK and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testPatchWarehouseActive_Success() throws Exception {
        ActiveToggleRequest request = new ActiveToggleRequest(false);
        WarehouseResponse inactiveResponse = new WarehouseResponse(
                1L, "WH-MAIN", "Entrepôt Principal", "Dépôt central", "Casablanca", false, true, Instant.now(), Instant.now());
        when(warehouseService.setActiveWarehouse(1L, false)).thenReturn(inactiveResponse);

        mockMvc.perform(patch("/api/warehouses/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(warehouseService).setActiveWarehouse(1L, false);
    }

    // ==========================================
    // Location Functional & Delegation Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/locations: returns paginated locations and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testListLocations_Success() throws Exception {
        Page<WarehouseLocationResponse> page = new PageImpl<>(List.of(sampleLocationResponse));
        when(warehouseService.listLocations(eq(1L), eq(true), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/warehouses/1/locations")
                        .param("active", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("LOC-GEN"))
                .andExpect(jsonPath("$.content[0].warehouseCode").value("WH-MAIN"));

        verify(warehouseService).listLocations(eq(1L), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/locations/{locationId}: returns location details and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testGetLocation_Success() throws Exception {
        when(warehouseService.getLocation(1L, 10L)).thenReturn(sampleLocationResponse);

        mockMvc.perform(get("/api/warehouses/1/locations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.code").value("LOC-GEN"))
                .andExpect(jsonPath("$.warehouseId").value(1));

        verify(warehouseService).getLocation(1L, 10L);
    }

    @Test
    @DisplayName("POST /api/warehouses/{warehouseId}/locations: ADMIN creates location with 201 CREATED and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testCreateLocation_Success() throws Exception {
        WarehouseLocationRequest request = new WarehouseLocationRequest("LOC-A1", "Allée A Rack 1", "Zone A");
        WarehouseLocationResponse createdResponse = new WarehouseLocationResponse(
                20L, 1L, "WH-MAIN", "Entrepôt Principal", "LOC-A1", "Allée A Rack 1", "Zone A", true, false, Instant.now(), Instant.now());
        when(warehouseService.createLocation(eq(1L), any(WarehouseLocationRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/warehouses/1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.code").value("LOC-A1"))
                .andExpect(jsonPath("$.warehouseId").value(1));

        verify(warehouseService).createLocation(eq(1L), refEq(request));
    }

    @Test
    @DisplayName("PUT /api/warehouses/{warehouseId}/locations/{locationId}: ADMIN updates location with 200 OK and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testUpdateLocation_Success() throws Exception {
        WarehouseLocationRequest request = new WarehouseLocationRequest("LOC-GEN-UPD", "Zone Rénovée", "Nouvelle desc");
        WarehouseLocationResponse updatedResponse = new WarehouseLocationResponse(
                10L, 1L, "WH-MAIN", "Entrepôt Principal", "LOC-GEN-UPD", "Zone Rénovée", "Nouvelle desc", true, true, Instant.now(), Instant.now());
        when(warehouseService.updateLocation(eq(1L), eq(10L), any(WarehouseLocationRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/warehouses/1/locations/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOC-GEN-UPD"))
                .andExpect(jsonPath("$.name").value("Zone Rénovée"));

        verify(warehouseService).updateLocation(eq(1L), eq(10L), refEq(request));
    }

    @Test
    @DisplayName("PATCH /api/warehouses/{warehouseId}/locations/{locationId}/active: ADMIN toggles active status with 200 OK and delegates")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testPatchLocationActive_Success() throws Exception {
        ActiveToggleRequest request = new ActiveToggleRequest(false);
        WarehouseLocationResponse inactiveResponse = new WarehouseLocationResponse(
                10L, 1L, "WH-MAIN", "Entrepôt Principal", "LOC-GEN", "Zone Générale", "Zone de stockage par défaut", false, true, Instant.now(), Instant.now());
        when(warehouseService.setActiveLocation(1L, 10L, false)).thenReturn(inactiveResponse);

        mockMvc.perform(patch("/api/warehouses/1/locations/10/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(warehouseService).setActiveLocation(1L, 10L, false);
    }

    // ==========================================
    // Validation Tests
    // ==========================================

    @Test
    @DisplayName("POST /api/warehouses: invalid WarehouseRequest returns 400 BAD_REQUEST")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testInvalidWarehouseRequest_ReturnsBadRequest() throws Exception {
        // Blank code and blank name
        WarehouseRequest invalidRequest = new WarehouseRequest("   ", "", null, null);

        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(warehouseService, never()).createWarehouse(any());
    }

    @Test
    @DisplayName("POST /api/warehouses/{warehouseId}/locations: invalid WarehouseLocationRequest returns 400 BAD_REQUEST")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testInvalidLocationRequest_ReturnsBadRequest() throws Exception {
        // Blank code and blank name
        WarehouseLocationRequest invalidRequest = new WarehouseLocationRequest("", "  ", null);

        mockMvc.perform(post("/api/warehouses/1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(warehouseService, never()).createLocation(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/warehouses/{id}/active: invalid ActiveToggleRequest (null active) returns 400 BAD_REQUEST")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testInvalidActiveToggleRequest_ReturnsBadRequest() throws Exception {
        // Missing active field
        String invalidJson = "{}";

        mockMvc.perform(patch("/api/warehouses/1/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(warehouseService, never()).setActiveWarehouse(any(), anyBoolean());
    }

    // ==========================================
    // Stock Visibility Endpoints Tests
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to GET /api/warehouses/1/stocks is rejected (401 or 403)")
    void testUnauthenticated_GetWarehouseStocks_Rejected() throws Exception {
        mockMvc.perform(get("/api/warehouses/1/stocks"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to GET /api/warehouses/stocks/articles/50 is rejected (401 or 403)")
    void testUnauthenticated_GetArticleStocks_Rejected() throws Exception {
        mockMvc.perform(get("/api/warehouses/stocks/articles/50"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/stocks: 200 OK with default parameters and user role allowed")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListWarehouseStocks_DefaultParams_Success() throws Exception {
        when(warehouseService.listWarehouseStocks(eq(1L), eq(null), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleStockResponse)));

        mockMvc.perform(get("/api/warehouses/1/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1000))
                .andExpect(jsonPath("$.content[0].warehouseId").value(1))
                .andExpect(jsonPath("$.content[0].warehouseCode").value("WH-MAIN"))
                .andExpect(jsonPath("$.content[0].locationId").value(10))
                .andExpect(jsonPath("$.content[0].locationCode").value("LOC-GEN"))
                .andExpect(jsonPath("$.content[0].articleId").value(50))
                .andExpect(jsonPath("$.content[0].articleReference").value("ART-001"))
                .andExpect(jsonPath("$.content[0].articleDesignation").value("Moteur Électrique"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Électronique"))
                .andExpect(jsonPath("$.content[0].unitName").value("Pièce"))
                .andExpect(jsonPath("$.content[0].quantity").value(20.0))
                .andExpect(jsonPath("$.content[0].minQuantity").value(5.0))
                .andExpect(jsonPath("$.content[0].purchasePriceHt").value(150.0))
                .andExpect(jsonPath("$.content[0].totalValueHt").value(3000.0))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(warehouseService).listWarehouseStocks(eq(1L), eq(null), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/stocks: forwards locationId, positiveOnly, and pagination parameters")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListWarehouseStocks_ForwardsAllParameters() throws Exception {
        when(warehouseService.listWarehouseStocks(eq(1L), eq(10L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleStockResponse), org.springframework.data.domain.PageRequest.of(2, 5), 15));

        mockMvc.perform(get("/api/warehouses/1/stocks")
                        .param("locationId", "10")
                        .param("positiveOnly", "true")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(15));

        verify(warehouseService).listWarehouseStocks(
                eq(1L),
                eq(10L),
                eq(true),
                argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 5)
        );
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/stocks: warehouse not found returns 404 NOT_FOUND")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListWarehouseStocks_WarehouseNotFound_Returns404() throws Exception {
        when(warehouseService.listWarehouseStocks(eq(99L), any(), anyBoolean(), any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Warehouse not found"));

        mockMvc.perform(get("/api/warehouses/99/stocks"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/warehouses/{warehouseId}/stocks: location mismatch returns 400 BAD_REQUEST")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListWarehouseStocks_LocationMismatch_Returns400() throws Exception {
        when(warehouseService.listWarehouseStocks(eq(1L), eq(999L), anyBoolean(), any(Pageable.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location does not belong to specified warehouse"));

        mockMvc.perform(get("/api/warehouses/1/stocks").param("locationId", "999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/warehouses/stocks/articles/{articleId}: 200 OK with articleId forwarded correctly")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListArticleStocks_Success() throws Exception {
        when(warehouseService.listArticleStocks(50L))
                .thenReturn(List.of(sampleStockResponse));

        mockMvc.perform(get("/api/warehouses/stocks/articles/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].articleId").value(50))
                .andExpect(jsonPath("$[0].warehouseCode").value("WH-MAIN"))
                .andExpect(jsonPath("$[0].totalValueHt").value(3000.0));

        verify(warehouseService).listArticleStocks(50L);
    }

    @Test
    @DisplayName("GET /api/warehouses/stocks/articles/{articleId}: article not found returns 404 NOT_FOUND")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testListArticleStocks_ArticleNotFound_Returns404() throws Exception {
        when(warehouseService.listArticleStocks(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));

        mockMvc.perform(get("/api/warehouses/stocks/articles/999"))
                .andExpect(status().isNotFound());
    }
}
