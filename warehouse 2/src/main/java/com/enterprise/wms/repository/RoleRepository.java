package com.enterprise.wms.repository;

import com.enterprise.wms.domain.WmsEnums.RoleName;
import com.enterprise.wms.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository for {@link Role} (ADMIN, MANAGER, WORKER). */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Find a role by its enum name (used when assigning roles to users). */
    Optional<Role> findByName(RoleName name);
}
