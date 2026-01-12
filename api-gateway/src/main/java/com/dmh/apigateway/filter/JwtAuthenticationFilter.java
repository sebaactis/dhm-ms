package com.dmh.apigateway.filter;

import com.dmh.apigateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/token/validate"
    );

    /**
     * Constructor para verificar que el filtro se inicializa correctamente
     */
    public JwtAuthenticationFilter() {
        logger.info("=================================================");
        logger.info("🚀 JwtAuthenticationFilter INITIALIZED");
        logger.info("🔒 Public routes: {}", PUBLIC_ROUTES);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String host = request.getHeaders().getFirst("Host");

        logger.info("=================================================");
        logger.info("🔒 JWT Filter - Incoming Request");
        logger.info("🔒 Method: {}, Path: {}, Host: {}", method, path, host);
        logger.info("🔒 Full URI: {}", request.getURI());
        logger.info("🔒 Query: {}", request.getQueryParams());
        logger.info("🔒 Headers: {}", request.getHeaders());

        boolean isPublic = isPublicRoute(path);
        logger.info("🔒 Is public route? {}", isPublic);
        logger.info("🔒 Public routes: {}", PUBLIC_ROUTES);

        if (isPublic) {
            logger.info("✅ PASSING: Public route, skipping authentication for {}", path);
            return chain.filter(exchange);
        }

        logger.warn("🚫 REQUIRING AUTH: This path needs JWT validation: {}", path);

        String authHeader = request.getHeaders().getFirst("Authorization");
        logger.info("🔒 Auth header present? {}", authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("❌ BLOCKED: Missing or invalid Authorization header for path: {}", path);
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        logger.info("🔒 Token extracted (first 20 chars): {}...", token.substring(0, Math.min(20, token.length())));

        if (!jwtUtil.validateToken(token)) {
            logger.error("❌ BLOCKED: Invalid or expired token for path: {}", path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        logger.info("✅ TOKEN VALIDATED: Token is valid");

        return isTokenInBlacklist(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        logger.warn("❌ BLOCKED: Token is blacklisted (user logged out) for path: {}", path);
                        return onError(exchange, "Token has been invalidated", HttpStatus.UNAUTHORIZED);
                    }

                    Long userId = jwtUtil.extractUserId(token);
                    logger.info("✅ ALLOWING: User ID {} validated successfully for path: {}", userId, path);

                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId.toString())
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                });
    }

    private boolean isPublicRoute(String path) {
        return PUBLIC_ROUTES.stream().anyMatch(path::startsWith);
    }

    private Mono<Boolean> isTokenInBlacklist(String token) {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/api/users/token/validate?token=" + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean isValid = (Boolean) response.get("valid");
                    return !isValid;
                })
                .onErrorResume(error -> {
                    logger.error("Error checking blacklist for token: {}", error.getMessage());
                    return Mono.just(true);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");
        
        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", 
                status.getReasonPhrase(), message);
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes())));
    }

    @Override
    public int getOrder() {
        // Filtro global debe tener orden positivo para ejecutarse después de filtros del sistema
        // pero ANTES de procesar la request
        return 0;
    }
}
