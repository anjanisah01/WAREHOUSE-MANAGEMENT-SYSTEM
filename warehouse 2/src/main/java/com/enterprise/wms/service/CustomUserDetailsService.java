package com.enterprise.wms.service;

import com.enterprise.wms.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a user from the database and maps their roles to Spring Security GrantedAuthorities.
 * Used by Spring Security's authentication manager during login.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepo;

    public CustomUserDetailsService(AppUserRepository userRepo) { this.userRepo = userRepo; }

    /** Loads user by username, converts roles to ROLE_* authorities for access control. */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Map each Role entity to a Spring Security "ROLE_<name>" authority
        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        return new User(user.getUsername(), user.getPasswordHash(), authorities);
    }
}
