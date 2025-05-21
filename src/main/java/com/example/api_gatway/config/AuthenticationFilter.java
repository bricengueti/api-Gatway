package com.example.api_gatway.config;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;


@RefreshScope
@Component
public class AuthenticationFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final RouterValidator routerValidator;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${auth-service.jwks-url:http://auth-service:8070/api/auth/.well-known/jwks}")
    private String jwksUrl;

    @Value("${auth-service.validate-url:http://auth-service:8070/api/auth/validate}")
    private String validateUrl;

    private volatile boolean publicKeyLoaded = false;

    public AuthenticationFilter(RouterValidator routerValidator, JwtUtil jwtUtil, RestTemplate restTemplate) {
        this.routerValidator = routerValidator;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        loadPublicKey();
    }

    private synchronized void loadPublicKey() {
        if (!publicKeyLoaded) {
            try {
                logger.info("Loading public key from: {}", jwksUrl);
                String pem = restTemplate.getForObject(jwksUrl, String.class);
                jwtUtil.setPublicKey(pem);
                publicKeyLoaded = true;
                logger.info("Public key loaded successfully");
            } catch (Exception e) {
                logger.error("Failed to load public key from Auth-Service. Will retry on first request. Error: {}", e.getMessage());
            }
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!routerValidator.isSecured.test(request)) {
            return chain.filter(exchange);
        }

        // Ensure public key is loaded
        if (!publicKeyLoaded) {
            loadPublicKey();
            if (!publicKeyLoaded) {
                logger.error("Auth-Service unavailable - could not load public key");
                return onError(exchange, HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        // Validate Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Local JWT validation
        if (jwtUtil.isInvalid(token)) {
            return onError(exchange, HttpStatus.FORBIDDEN);
        }

        // Remote token validation
        return validateTokenWithAuthService(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    }

                    // Add user info to headers
                    Claims claims = jwtUtil.getAllClaimsFromToken(token);
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", String.valueOf(claims.get("user_id")))
                            .header("X-Username", claims.getSubject())
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    logger.error("Error validating token with Auth-Service: {}", e.getMessage());
                    return onError(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    private Mono<Boolean> validateTokenWithAuthService(String token) {
        return Mono.fromCallable(() -> {
            String validationUrl = UriComponentsBuilder
                    .fromHttpUrl(validateUrl)
                    .queryParam("token", token)
                    .toUriString();

            logger.debug("Validating token with Auth-Service at: {}", validationUrl);
            Boolean isValid = restTemplate.getForObject(validationUrl, Boolean.class);
            return Boolean.TRUE.equals(isValid);
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}