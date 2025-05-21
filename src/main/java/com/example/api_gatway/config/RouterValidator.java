package com.example.api_gatway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    // Use more flexible path matching
    public static final List<String> openApiEndpoints = List.of(
            // Auth endpoints

            "/api/auth/v3/api-docs",
            "/api/auth/",
            "/auth-service/api/auth/",

            // product endpoints
            "/api/products/v3/api-docs",
            "/product-service/api/products/",

            // Swagger/OpenAPI endpoints
            "/v3/api-docs",
            "/v3/api-docs/",
            "/swagger-ui",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/webjars/",
            "/swagger-resources",
            "/swagger-resources/",

            // Actuator endpoints
            "/actuator",
            "/actuator/",
            "/actuator/health",
            "/actuator/info"
    );

    public final Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();

                // Special case for empty path
                if (path.equals("") || path.equals("/")) {
                    return false;
                }

                // Check against all open endpoints
                return openApiEndpoints.stream()
                        .noneMatch(openPath ->
                                path.equals(openPath) ||
                                        path.startsWith(openPath) ||
                                        (openPath.endsWith("/") && path.startsWith(openPath)) ||
                                        (!openPath.endsWith("/") && path.startsWith(openPath + "/")));
            };
}