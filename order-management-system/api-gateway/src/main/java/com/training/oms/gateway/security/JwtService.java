package com.training.oms.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Minimal, educational JWT issuer/validator (Advanced module: Security).
 * In a production system this responsibility belongs to a dedicated identity
 * provider (Keycloak, Azure AD / Entra ID, Auth0, etc.) speaking OAuth2 /
 * OIDC - this class exists purely so the course can demonstrate the gateway
 * filter mechanics without standing up an external IdP.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
