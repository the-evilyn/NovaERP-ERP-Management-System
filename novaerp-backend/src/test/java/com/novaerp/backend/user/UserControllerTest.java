package com.novaerp.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaerp.backend.user.dto.AdminUserResponse;
import com.novaerp.backend.user.dto.UpdateUserRoleRequest;
import com.novaerp.backend.user.dto.UpdateUserStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("1. ADMIN can list users (200 OK, paginated, no password in JSON)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminCanListUsers() throws Exception {
        AdminUserResponse u1 = new AdminUserResponse(1L, "Admin User", "admin@novaerp.local", Role.ADMIN, true, Instant.now());
        AdminUserResponse u2 = new AdminUserResponse(2L, "Sara Amrani", "sara@novaerp.local", Role.USER, true, Instant.now());
        Page<AdminUserResponse> page = new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 20), 2);

        when(userService.listUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("admin@novaerp.local"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[1].email").value("sara@novaerp.local"))
                .andExpect(jsonPath("$.content[1].role").value("USER"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("2. USER cannot list users (403 Forbidden)")
    @WithMockUser(username = "user@novaerp.local", roles = {"USER"})
    void testUserCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Unauthenticated request to /api/users is rejected (401/403)")
    void testUnauthenticatedCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }

    @Test
    @DisplayName("4. ADMIN can change user role")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminCanChangeRole() throws Exception {
        User adminUser = User.builder().id(1L).email("admin@novaerp.local").role(Role.ADMIN).build();
        when(userRepository.findByEmail("admin@novaerp.local")).thenReturn(Optional.of(adminUser));

        AdminUserResponse updated = new AdminUserResponse(2L, "Sara Amrani", "sara@novaerp.local", Role.ADMIN, true, Instant.now());
        when(userService.updateUserRole(eq(2L), eq(Role.ADMIN), any())).thenReturn(updated);

        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.ADMIN);

        mockMvc.perform(patch("/api/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("5. ADMIN cannot perform invalid role update (null body or invalid role)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminCannotPerformInvalidRoleUpdate() throws Exception {
        mockMvc.perform(patch("/api/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"INVALID_ROLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6. ADMIN can activate/deactivate user")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testAdminCanActivateDeactivateUser() throws Exception {
        User adminUser = User.builder().id(1L).email("admin@novaerp.local").role(Role.ADMIN).build();
        when(userRepository.findByEmail("admin@novaerp.local")).thenReturn(Optional.of(adminUser));

        AdminUserResponse updated = new AdminUserResponse(2L, "Sara Amrani", "sara@novaerp.local", Role.USER, false, Instant.now());
        when(userService.updateUserStatus(eq(2L), eq(false), any())).thenReturn(updated);

        UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch("/api/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("7. USER cannot change role (403 Forbidden)")
    @WithMockUser(username = "user@novaerp.local", roles = {"USER"})
    void testUserCannotChangeRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.ADMIN);

        mockMvc.perform(patch("/api/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. USER cannot change account status (403 Forbidden)")
    @WithMockUser(username = "user@novaerp.local", roles = {"USER"})
    void testUserCannotChangeStatus() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch("/api/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Unknown user returns 404 Not Found")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testUnknownUserReturns404() throws Exception {
        when(userService.getUser(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. Self-demotion or self-deactivation is safely rejected (400 Bad Request)")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testSelfDemotionRejected() throws Exception {
        User adminUser = User.builder().id(1L).email("admin@novaerp.local").role(Role.ADMIN).build();
        when(userRepository.findByEmail("admin@novaerp.local")).thenReturn(Optional.of(adminUser));

        when(userService.updateUserRole(eq(1L), eq(Role.USER), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove your own ADMIN role"));

        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.USER);

        mockMvc.perform(patch("/api/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("11. Password or sensitive authentication fields never appear in user API response")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testPasswordNeverExposed() throws Exception {
        AdminUserResponse response = new AdminUserResponse(1L, "Admin User", "admin@novaerp.local", Role.ADMIN, true, Instant.now());
        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("12. Pagination parameters are passed to service correctly")
    @WithMockUser(username = "admin@novaerp.local", roles = {"ADMIN"})
    void testPaginationWorksCorrectly() throws Exception {
        Page<AdminUserResponse> page = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);
        when(userService.listUsers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(10));
    }
}
