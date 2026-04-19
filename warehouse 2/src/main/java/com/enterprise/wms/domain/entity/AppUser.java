package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * An application user account with one or more roles (ADMIN, MANAGER, WORKER).
 * Passwords are stored as BCrypt hashes.
 */
@Entity
@Getter @Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)      // Login username — must be unique
    private String username;

    @Column(nullable = false)                     // BCrypt-hashed password
    private String passwordHash;

    @ManyToMany(fetch = FetchType.EAGER)          // Eagerly loaded role set for Spring Security
    @JoinTable(
        name = "user_roles",                     // Join table linking users ↔ roles
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();    // Roles assigned to this user
}
