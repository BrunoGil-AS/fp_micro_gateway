package com.aspiresys.fp_micro_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF
                .authorizeExchange(exchanges -> exchanges
                        // Permitir acceso público a endpoints del auth service
                        .pathMatchers("/auth/**").permitAll()
                        // Permitir acceso a endpoints de actuator para health checks
                        .pathMatchers("/actuator/**").permitAll()
                        // Cualquier otra petición requiere autenticación
                        .anyExchange().authenticated()
                )
                // Configurar como servidor de recursos OAuth2 con JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
