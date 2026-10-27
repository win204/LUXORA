package com.luxora.commerce.user.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.user.dto.AdminCreateUserRequest;
import com.luxora.commerce.user.dto.AdminUpdateUserStatusRequest;
import com.luxora.commerce.user.dto.AdminUserResponse;
import com.luxora.commerce.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "Admin user account management: list, create, lock/unlock, delete")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Returns a list of all registered users ordered by creation date descending.")
    public ResponseEntity<List<AdminUserResponse>> listAll() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new client or administrator account.")
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserResponse created = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Lock or unlock user account", description = "Toggles the enabled status of a user account. Disabling revokes active sessions.")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody AdminUpdateUserStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentAdmin) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request.enabled(), currentAdmin));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account", description = "Permanently deletes a user account while preserving financial and order history.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser currentAdmin) {
        adminUserService.deleteUser(id, currentAdmin);
        return ResponseEntity.noContent().build();
    }
}
