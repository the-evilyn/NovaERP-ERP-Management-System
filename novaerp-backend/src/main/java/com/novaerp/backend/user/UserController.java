package com.novaerp.backend.user;

import com.novaerp.backend.user.dto.AdminUserResponse;
import com.novaerp.backend.user.dto.UpdateUserRoleRequest;
import com.novaerp.backend.user.dto.UpdateUserStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Administrative user management endpoints")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List all users with pagination (ADMIN only)")
    public Page<AdminUserResponse> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return userService.listUsers(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID (ADMIN only)")
    public AdminUserResponse getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role (ADMIN only)")
    public AdminUserResponse updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        User currentUser = resolveUser(authentication);
        return userService.updateUserRole(id, request.role(), currentUser);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user active/enabled status (ADMIN only)")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        User currentUser = resolveUser(authentication);
        return userService.updateUserStatus(id, request.enabled(), currentUser);
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof User u) {
            return u;
        }
        if (authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return null;
    }
}
