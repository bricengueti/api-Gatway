package com.example.api_gatway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://auth-service"))
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://product-service"))
                .build();
    }
}
