package com.enterprise.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 * - Disables CSRF (stateless API with JWT)
 * - Sets up URL-based role authorisation
 * - Registers the JWT filter ahead of the default username/password filter
 * - Provides BCrypt password encoder and DAO auth provider beans
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;         // extracts JWT from requests
    private final UserDetailsService userDetailsService; // loads users from the DB

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    /** Defines the HTTP security filter chain. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())                            // stateless API — no CSRF needed
                .headers(headers -> headers
                        .contentTypeOptions(opt -> {})               // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.sameOrigin())   // allow iframes from same origin
                        .xssProtection(xss -> {}))                   // X-XSS-Protection header
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/app", "/error", "/api/auth/**").permitAll()  // public endpoints
                        .requestMatchers("/api/users/**").hasRole("ADMIN")                             // user mgmt → admin only
                        .requestMatchers("/api/wms/analytics/**").hasAnyRole("ADMIN", "MANAGER")       // analytics → admin/manager
                        .requestMatchers("/api/wms/audit/**").hasAnyRole("ADMIN", "MANAGER")           // audit logs → admin/manager
                        .requestMatchers("/api/wms/alerts/**").hasAnyRole("ADMIN", "MANAGER")          // alerts → admin/manager
                        .requestMatchers("/api/wms/**").authenticated()                                // all other WMS endpoints
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // no HTTP sessions
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);  // JWT runs first
        return http.build();
    }

    /** DAO-based authentication provider using BCrypt and our UserDetailsService. */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Exposes the authentication manager for use in AuthController. */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /** BCrypt password encoder bean. */
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
