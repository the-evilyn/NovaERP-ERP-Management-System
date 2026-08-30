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
}
