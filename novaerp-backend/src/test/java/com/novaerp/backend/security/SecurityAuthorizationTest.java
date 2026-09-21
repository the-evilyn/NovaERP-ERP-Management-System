package com.novaerp.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ROLE_USER can POST to /api/stock/movements (not 403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CanPostStockMovement() throws Exception {
        // Will fail with 404 (article not found) or 400 (validation), but NOT 403 Forbidden
        mockMvc.perform(post("/api/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":999999,\"type\":\"IN\",\"quantity\":1}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status)
                            .isNotEqualTo(403)
                            .isNotEqualTo(401);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/stock/articles (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostArticle() throws Exception {
        mockMvc.perform(post("/api/stock/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"TEST\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/stock/articles (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostArticle() throws Exception {
        mockMvc.perform(post("/api/stock/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reference\":\"TEST\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/stock/movements is blocked")
    void testUnauthenticated_Blocked() throws Exception {
        mockMvc.perform(post("/api/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":1,\"type\":\"IN\",\"quantity\":1}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/clients (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostClient() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Client\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/clients (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostClient() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Admin Client\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/auth/me is blocked")
    void testUnauthenticated_CannotAccessAuthMe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER can access POST /api/sales/orders (not 403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CanPostSaleOrder() throws Exception {
        mockMvc.perform(post("/api/sales/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"items\":[]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/sales/orders/1/confirm (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotConfirmSaleOrder() throws Exception {
        mockMvc.perform(post("/api/sales/orders/1/confirm"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/sales/orders (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostSaleOrder() throws Exception {
        mockMvc.perform(post("/api/sales/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"items\":[]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/sales/orders is blocked")
    void testSalesOrders_UnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/sales/orders"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/purchases/orders (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostPurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/purchases/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":1,\"items\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/purchases/orders (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostPurchaseOrder() throws Exception {
        mockMvc.perform(post("/api/purchases/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":1,\"items\":[]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/purchases/orders is blocked")
    void testPurchasesOrders_UnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/purchases/orders"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/invoices/customers (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostCustomerInvoice() throws Exception {
        mockMvc.perform(post("/api/invoices/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"items\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/invoices/customers (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostCustomerInvoice() throws Exception {
        mockMvc.perform(post("/api/invoices/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"items\":[]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/invoices/customers is blocked")
    void testCustomerInvoices_UnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/invoices/customers"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/invoices/suppliers (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostSupplierInvoice() throws Exception {
        mockMvc.perform(post("/api/invoices/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":1,\"items\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/invoices/suppliers (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostSupplierInvoice() throws Exception {
        mockMvc.perform(post("/api/invoices/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":1,\"items\":[]}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/invoices/suppliers is blocked")
    void testSupplierInvoices_UnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/invoices/suppliers"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("ROLE_USER cannot POST to /api/payments/customer-invoices/1 (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotPostPayment() throws Exception {
        mockMvc.perform(post("/api/payments/customer-invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"paymentMethod\":\"CASH\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_USER cannot DELETE to /api/payments/1 (403 Forbidden)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CannotDeletePayment() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/payments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN can access POST /api/payments/customer-invoices/1 (not 403 Forbidden)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminRole_CanPostPayment() throws Exception {
        mockMvc.perform(post("/api/payments/customer-invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100,\"paymentMethod\":\"CASH\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("ROLE_USER can GET /api/payments (not 403 or 401)")
    @WithMockUser(username = "warehouse@novaerp.local", roles = {"USER"})
    void testUserRole_CanGetPayments() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    @DisplayName("Unauthenticated request to /api/payments is blocked")
    void testPayments_UnauthenticatedBlocked() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }
}
