package com.training.oms.gateway.security;

import io.jsonwebtoken.JwtException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cross-cutting authentication enforced once, at the edge, instead of in
 * every downstream microservice (Advanced module: Security).
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login", "/actuator",
            // Swagger UI + OpenAPI docs (Advanced lesson: API documentation) are
            // intentionally left open so students can browse the API without a token.
            "/swagger-ui", "/v3/api-docs", "/docs/", "/webjars");

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }

        try {
            String token = authHeader.substring("Bearer ".length());
            String username = jwtService.parseClaims(token).getSubject();
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Authenticated-User", username)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException ex) {
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        DataBuffer buffer = response.bufferFactory()
                .wrap(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
