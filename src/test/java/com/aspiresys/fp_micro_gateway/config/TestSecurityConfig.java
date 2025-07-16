package com.aspiresys.fp_micro_gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import java.util.List;

import java.time.Instant;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public ReactiveJwtDecoder mockJwtDecoder() {
        return token -> {
            if ("token".equals(token)) {
                return Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "user123")
                    .claim("roles", List.of("ROLE_USER"))
                    .build());
            }
            return Mono.error(new RuntimeException("Invalid token"));
        };
    }

    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/gateway/health/public").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
            .build();
    }
}
