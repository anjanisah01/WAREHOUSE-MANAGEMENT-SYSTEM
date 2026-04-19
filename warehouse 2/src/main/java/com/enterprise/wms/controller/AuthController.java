package com.enterprise.wms.controller;

import com.enterprise.wms.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoint — handles login requests and returns JWT tokens.
 * POST /api/auth/login  { "username": "...", "password": "..." }  →  { "token": "..." }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager; // Spring Security auth manager
    private final JwtService jwtService;             // generates/validates JWTs

    public AuthController(AuthenticationManager authManager, JwtService jwtService) {
        this.authManager = authManager;
        this.jwtService  = jwtService;
    }

    /** Authenticates the user and returns a signed JWT on success. */
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {
        // Delegate credential validation to Spring Security
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.get("username"), body.get("password")));
        // If no exception was thrown, credentials are valid → issue a token
        return Map.of("token", jwtService.generateToken(body.get("username")));
    }
}
