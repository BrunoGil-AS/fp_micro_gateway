package com.aspiresys.fp_micro_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/gateway")
public class GatewayTestController {

    /**
     * Test endpoint to verify JWT authentication.
     * <p>
     * Requires the user to have the 'USER' role.
     * Returns a JSON object indicating authentication status, the user's subject,
     * roles, and a message.
     *
     * @param jwt the authenticated user's JWT token, or null if unauthenticated
     * @return a Mono containing a map with authentication details
     */
    
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/test")
    public Mono<Map<String, Object>> test(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return Mono.just(Map.of(
                "status", "unauthenticated",
                "message", "No JWT token provided"
            ));
        }
        
        return Mono.just(Map.of(
            "status", "authenticated", 
            "user", jwt.getSubject(),
            "roles", jwt.getClaimAsStringList("roles"),
            "message", "Gateway authentication working correctly"
        ));
    }

    /**
     * Health check endpoint for administrators.
     * <p>
     * Requires the user to have the 'ADMIN' role.
     * Returns a JSON object with service status, timestamp, service name,
     * authenticated user's name, authorities, and a message.
     *
     * @param authentication the authentication object containing user details
     * @return a Mono containing a ResponseEntity with health check information
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> adminHealthCheck(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Gateway");
        response.put("user", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("message", "Gateway funcionando correctamente - Acceso autorizado para ADMIN");
        
        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Public health check endpoint.
     * <p>
     * Does not require authentication.
     * Returns a JSON object with service status, timestamp, service name, and a message.
     *
     * @return a Mono containing a ResponseEntity with public health check information
     */
    @GetMapping("/health/public")
    public Mono<ResponseEntity<Map<String, Object>>> publicHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Gateway");
        response.put("message", "Gateway funcionando correctamente - Acceso público");
        
        return Mono.just(ResponseEntity.ok(response));
    }

    /**
     * Health check endpoint for users and administrators.
     * <p>
     * Requires the user to have either the 'USER' or 'ADMIN' role.
     * Returns a JSON object with service status, timestamp, service name,
     * authenticated user's name, authorities, and a message.
     *
     * @param authentication the authentication object containing user details
     * @return a Mono containing a ResponseEntity with health check information
     */
    @GetMapping("/health/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> userHealthCheck(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Gateway");
        response.put("user", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("message", "Gateway funcionando correctamente - Acceso autorizado para USER/ADMIN");
        
        return Mono.just(ResponseEntity.ok(response));
    }
}
