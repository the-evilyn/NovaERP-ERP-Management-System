package com.novaerp.backend.user;

import com.novaerp.backend.user.dto.AdminUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long targetUserId, Role newRole, User authenticatedUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (authenticatedUser != null && authenticatedUser.getId() != null
                && authenticatedUser.getId().equals(targetUserId)
                && newRole != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove your own ADMIN role");
        }

        targetUser.setRole(newRole);
        User saved = userRepository.save(targetUser);
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long targetUserId, boolean enabled, User authenticatedUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (authenticatedUser != null && authenticatedUser.getId() != null
                && authenticatedUser.getId().equals(targetUserId)
                && !enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate your own account");
        }

        targetUser.setEnabled(enabled);
        User saved = userRepository.save(targetUser);
        return AdminUserResponse.from(saved);
    }
}
