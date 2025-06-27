package com.aspiresys.fp_micro_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
public class GatewayTestController {

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

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
            "status", "UP",
            "service", "Gateway"
        ));
    }
}
