package com.enterprise.wms.controller;

import com.enterprise.wms.domain.WmsEnums.RoleName;
import com.enterprise.wms.domain.entity.AppUser;
import com.enterprise.wms.dto.UserDtos.CreateUserRequest;
import com.enterprise.wms.dto.UserDtos.UpdateRolesRequest;
import com.enterprise.wms.dto.UserDtos.UserResponse;
import com.enterprise.wms.service.UserManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRUD REST controller for user management (ADMIN-only, secured in SecurityConfig).
 * Base path: /api/users
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementService userService;

    public UserController(UserManagementService userService) { this.userService = userService; }

    /** GET /api/users — list all users. */
    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers().stream().map(this::toResponse).toList();
    }

    /** POST /api/users — create a new user. */
    @PostMapping
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        AppUser user = userService.createUser(request.username(), request.password(), parseRoles(request.roles()));
        return toResponse(user);
    }

    /** DELETE /api/users/{id} — delete a user. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /** PUT /api/users/{id}/roles — replace a user's roles. */
    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @RequestBody UpdateRolesRequest request) {
        return toResponse(userService.updateRoles(id, parseRoles(request.roles())));
    }

    /** Maps an AppUser entity to a safe DTO (no password hash exposed). */
    private UserResponse toResponse(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new UserResponse(user.getId(), user.getUsername(), roles);
    }

    /** Normalises role strings into RoleName enums; defaults to WORKER if none given. */
    private Set<RoleName> parseRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) return Set.of(RoleName.WORKER);
        return roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> RoleName.valueOf(r.trim().toUpperCase()))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
