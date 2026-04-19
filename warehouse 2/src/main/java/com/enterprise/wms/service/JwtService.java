package com.enterprise.wms.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility — generates, parses, and validates JSON Web Tokens for authentication.
 * Uses HMAC-SHA256. If the configured secret is shorter than 32 bytes it is right-padded.
 */
@Service
public class JwtService {

    @Value("${wms.security.jwt-secret}")  private String secret;   // signing key from config
    @Value("${wms.security.jwt-expiry-ms}") private long expiryMs; // token lifetime in milliseconds

    /** Creates a signed JWT for the given username. */
    public String generateToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiryMs); // token expires after expiryMs
        return Jwts.builder()
                .subject(username)      // "sub" claim
                .issuedAt(now)          // "iat" claim
                .expiration(expiry)     // "exp" claim
                .signWith(secretKey())  // sign with HMAC key
                .compact();
    }

    /** Extracts the username (subject) from a token. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token's subject matches the username and it hasn't expired. */
    public boolean isTokenValid(String token, String username) {
        Claims claims = parseClaims(token);
        return username.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    /** Parses and verifies the token, returning its claims. */
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
    }

    /** Derives the HMAC-SHA key, padding the secret to 256 bits if it's too short. */
    private SecretKey secretKey() {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(key.length >= 32 ? key : String.format("%-32s", secret).getBytes(StandardCharsets.UTF_8));
    }
}
