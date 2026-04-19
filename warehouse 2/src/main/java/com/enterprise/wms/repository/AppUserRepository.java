package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository for {@link AppUser} entities. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Look up a user by their unique username (used for login). */
    Optional<AppUser> findByUsername(String username);
}
