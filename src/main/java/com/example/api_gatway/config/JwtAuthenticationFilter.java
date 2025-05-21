//package com.example.api_gatway.config;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.security.KeyFactory;
//import java.security.PublicKey;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
//@Component
//public class JwtAuthenticationFilter implements WebFilter {
//
//    @Value("${auth-service.jwks-url:http://auth-service/api/auth/.well-known/jwks}")
//    private String jwksUrl;
//
//    @Value("${auth-service.validate-url:http://auth-service/api/auth/validate}")
//    private String validateUrl;
//
//    private final WebClient webClient = WebClient.create();
//    private final RestTemplate restTemplate;
//    private volatile PublicKey publicKey;
//
//    public JwtAuthenticationFilter(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
//
//    private boolean isPublicRoute(String path) {
//        return path.equals("/") ||
//                path.equals("/favicon.ico") ||
//                path.startsWith("/auth-service/api/auth") ||
//                path.startsWith("/api/auth/v3/api-docs") ||
//                path.startsWith("/api/auth/swagger-ui") ||
//                path.startsWith("/auth-service/api/auth/v3/api-docs") ||  // <- Ajoute celle-ci
//                path.startsWith("/auth-service/api/auth/swagger-ui") ||   // <- Et celle-ci
//                path.startsWith("/product-service/api/products/v3/api-docs") ||  // <- Ajoute celle-ci
//                path.startsWith("/product-service/api/products/swagger-ui") ||   // <- Et celle-ci
//                path.startsWith("/v3/api-docs") ||
//                path.startsWith("/swagger-ui") ||
//                path.startsWith("/swagger-resources") ||
//                path.startsWith("/webjars");
//    }
//
//
//    private Mono<PublicKey> getOrLoadPublicKey() {
//        if (publicKey != null) return Mono.just(publicKey);
//
//        return webClient.get()
//                .uri(jwksUrl)
//                .retrieve()
//                .bodyToMono(String.class)
//                .map(this::parsePublicKey)
//                .doOnNext(key -> this.publicKey = key);
//    }
//
//    private PublicKey parsePublicKey(String pem) {
//        try {
//            String publicKeyPEM = pem
//                    .replace("-----BEGIN PUBLIC KEY-----", "")
//                    .replace("-----END PUBLIC KEY-----", "")
//                    .replaceAll("\\s", "");
//
//            byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
//            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
//            return KeyFactory.getInstance("RSA").generatePublic(spec);
//        } catch (Exception e) {
//            throw new RuntimeException("Erreur parsing clé publique", e);
//        }
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        String path = request.getPath().value();
//
//        if (isPublicRoute(path)) {
//            return chain.filter(exchange);
//        }
//
//        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
//            return exchange.getResponse().setComplete();
//        }
//
//        String token = authHeader.substring(7);
//
//        return getOrLoadPublicKey()
//                .flatMap(pubKey -> {
//                    try {
//                        Claims claims = Jwts.parserBuilder()
//                                .setSigningKey(pubKey)
//                                .build()
//                                .parseClaimsJws(token)
//                                .getBody();
//
//                        Boolean isValid = restTemplate.getForObject(validateUrl + "?token=" + token, Boolean.class);
//                        if (Boolean.FALSE.equals(isValid)) {
//                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                            exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
//                            return exchange.getResponse().setComplete();
//                        }
//
//                        ServerHttpRequest modifiedRequest = request.mutate()
//                                .header("X-User-Id", String.valueOf(claims.get("user_id")))
//                                .header("X-Username", claims.getSubject())
//                                .build();
//
//                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
//
//                    } catch (Exception e) {
//                        System.err.println("❌ Erreur JWT : " + e.getMessage());
//                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
//                        return exchange.getResponse().setComplete();
//                    }
//                })
//                .onErrorResume(err -> {
//                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                    exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
//                    return exchange.getResponse().setComplete();
//                });
//    }
//}
