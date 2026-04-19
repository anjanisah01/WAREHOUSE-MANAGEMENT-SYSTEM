package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.RoleName;
import com.enterprise.wms.domain.entity.AppUser;
import com.enterprise.wms.domain.entity.Role;
import com.enterprise.wms.repository.AppUserRepository;
import com.enterprise.wms.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CRUD operations for application users (admin-only).
 * Handles creation, deletion (with self-delete protection), and role updates.
 */
@Service
public class UserManagementService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder encoder; // BCrypt encoder

    public UserManagementService(AppUserRepository userRepo, RoleRepository roleRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.encoder  = encoder;
    }

    /** Returns all users. */
    public List<AppUser> listUsers() { return userRepo.findAll(); }

    /** Creates a new user with the given roles (defaults to WORKER if none provided). */
    @Transactional
    public AppUser createUser(String username, String rawPassword, Set<RoleName> roleNames) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
        if (rawPassword == null || rawPassword.isBlank()) throw new IllegalArgumentException("Password is required");
        if (userRepo.findByUsername(username.trim()).isPresent())
            throw new IllegalArgumentException("Username already exists: " + username);

        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setPasswordHash(encoder.encode(rawPassword)); // hash the password
        user.setRoles(resolveRoles(roleNames));             // look up or create role entities
        return userRepo.save(user);
    }

    /** Deletes a user by ID. Prevents the currently logged-in user from deleting themselves. */
    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get the current principal's username to prevent self-deletion
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth == null ? "" : auth.getName();
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(currentUser))
            throw new IllegalArgumentException("You cannot delete your own account");

        userRepo.delete(user);
    }

    /** Replaces a user's role set. */
    @Transactional
    public AppUser updateRoles(Long userId, Set<RoleName> roleNames) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRoles(resolveRoles(roleNames));
        return userRepo.save(user);
    }

    /** Converts role names to Role entities, creating any that don't yet exist in the DB. */
    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<RoleName> requested = (names == null || names.isEmpty()) ? Set.of(RoleName.WORKER) : names;
        Set<Role> roles = new HashSet<>();
        for (RoleName rn : requested) {
            roles.add(roleRepo.findByName(rn).orElseGet(() -> {
                Role r = new Role();
                r.setName(rn);
                return roleRepo.save(r);
            }));
        }
        return roles;
    }
}
