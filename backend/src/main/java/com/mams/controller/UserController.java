package com.mams.controller;

import com.mams.dto.common.ApiResponse;
import com.mams.dto.user.CreateUserRequest;
import com.mams.dto.user.ResetPasswordRequest;
import com.mams.dto.user.UpdateUserRequest;
import com.mams.dto.user.UserDTO;
import com.mams.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User administration endpoints (ADMIN only)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new user account (Admin only)")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        UserDTO created = userService.createUser(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @GetMapping
    @Operation(summary = "List users with optional role, base, and active filters (Admin only)")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getUsers(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "base_id", required = false) Long baseIdParam,
            @RequestParam(value = "baseId", required = false) Long baseIdCamel,
            @RequestParam(value = "is_active", required = false) Boolean isActiveParam,
            @RequestParam(value = "isActive", required = false) Boolean isActiveCamel,
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            Authentication authentication) {

        Long baseId = baseIdParam != null ? baseIdParam : baseIdCamel;
        Boolean isActive = isActiveParam != null ? isActiveParam : isActiveCamel;

        Page<UserDTO> users = userService.getUsers(role, baseId, isActive, pageable, authentication);
        return ResponseEntity.ok(ApiResponse.of(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        UserDTO user = userService.getUserById(id, authentication);
        return ResponseEntity.ok(ApiResponse.of(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details, role, base, and active status (Admin only)")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        UserDTO updated = userService.updateUser(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account (Admin only)")
    public ResponseEntity<ApiResponse<String>> deactivateUser(
            @PathVariable Long id,
            Authentication authentication) {
        userService.deactivateUser(id, authentication);
        return ResponseEntity.ok(ApiResponse.of("User deactivated successfully"));
    }

    @PatchMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password (Admin only)")
    public ResponseEntity<ApiResponse<UserDTO>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication) {
        UserDTO updated = userService.resetPassword(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.of(updated));
    }
}
