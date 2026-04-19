package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.RoleName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A security role (ADMIN, MANAGER, or WORKER).
 * Mapped to the "app_role" table; linked to users via the "user_roles" join table.
 */
@Entity
@Getter @Setter
@Table(name = "app_role")                        // Custom table name to avoid SQL reserved word
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)                 // Stored as a string ("ADMIN", "MANAGER", etc.)
    @Column(unique = true)                       // Each role name appears only once
    private RoleName name;
}
