package com.luxora.commerce.user.service;

import com.luxora.commerce.auth.repository.RefreshTokenRepository;
import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.user.dto.AdminCreateUserRequest;
import com.luxora.commerce.user.dto.AdminUserResponse;
import com.luxora.commerce.user.model.Role;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.RoleRepository;
import com.luxora.commerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserMapper::toAdminUser)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "Email này đã được đăng ký trong hệ thống");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(email, passwordHash, request.firstName().trim(), request.lastName().trim());

        // Default role is ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        user.addRole(userRole);

        if ("ROLE_ADMIN".equalsIgnoreCase(request.role()) || "ADMIN".equalsIgnoreCase(request.role())) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            user.addRole(adminRole);
        }

        User saved = userRepository.save(user);
        return UserMapper.toAdminUser(saved);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, boolean enabled, AuthenticatedUser currentAdmin) {
        if (currentAdmin != null && userId.equals(currentAdmin.id()) && !enabled) {
            throw new BadRequestException("CANNOT_LOCK_SELF", "Quản trị viên không thể tự khóa tài khoản của chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        if (!enabled && "admin@luxora.com".equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("CANNOT_LOCK_PRIMARY_ADMIN", "Không thể khóa tài khoản quản trị viên chính");
        }

        user.setEnabled(enabled);
        User updated = userRepository.save(user);

        if (!enabled) {
            // Revoke active sessions when locked
            refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
        }

        return UserMapper.toAdminUser(updated);
    }

    @Transactional
    public void deleteUser(UUID userId, AuthenticatedUser currentAdmin) {
        if (currentAdmin != null && userId.equals(currentAdmin.id())) {
            throw new BadRequestException("CANNOT_DELETE_SELF", "Quản trị viên không thể tự xóa tài khoản của chính mình");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        if ("admin@luxora.com".equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("CANNOT_DELETE_PRIMARY_ADMIN", "Không thể xóa tài khoản quản trị viên chính");
        }

        // Dissociate past orders so order history remains intact without foreign key violation
        jdbcTemplate.update("UPDATE orders SET user_id = NULL WHERE user_id = ?", userId);

        // Delete user's active refresh tokens
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE user_id = ?", userId);

        // Delete user roles
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", userId);

        // Delete user
        userRepository.delete(user);
    }
}
