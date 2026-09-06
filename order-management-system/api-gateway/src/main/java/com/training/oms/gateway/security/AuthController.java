package com.training.oms.gateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Demo login endpoint backed by an in-memory user store. Publicly reachable
 * (excluded from {@link JwtAuthenticationFilter}) so clients can obtain a
 * token before calling protected routes.
 */
@RestController
public class AuthController {

    private static final Map<String, String> DEMO_USERS = Map.of(
            "admin", "admin123",
            "customer", "customer123"
    );

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        String expectedPassword = DEMO_USERS.get(request.username());
        if (expectedPassword == null || !expectedPassword.equals(request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
        List<String> roles = "admin".equals(request.username()) ? List.of("ADMIN") : List.of("CUSTOMER");
        String token = jwtService.generateToken(request.username(), roles);
        return ResponseEntity.ok(Map.of("token", token, "tokenType", "Bearer"));
    }
}
