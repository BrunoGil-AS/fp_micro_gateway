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

    @PreAuthorize("hasRole('USER')")
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
     * Endpoint de health check que requiere rol ADMIN.
     * Utilizado para verificar que la autorización basada en roles funciona correctamente.
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
     * Endpoint de health check público para verificación básica.
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
     * Endpoint que requiere cualquier usuario autenticado.
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
