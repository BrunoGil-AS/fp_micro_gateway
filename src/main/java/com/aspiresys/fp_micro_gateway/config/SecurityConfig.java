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
                        .pathMatchers("/auth/**").permitAll() // Permite acceso libre al auth service
                        .pathMatchers("/oauth2/**").permitAll() // Permite acceso libre a OAuth2 endpoints
                        .pathMatchers("/login").permitAll() // Permite acceso libre al login
                        .pathMatchers("/actuator/**").permitAll() // Permite acceso libre a actuator
                        .pathMatchers("/gateway/health").permitAll() // Permite acceso libre al health check
                        .anyExchange().authenticated() // Requiere autenticación para todo lo demás
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwkSetUri("http://localhost:8081/oauth2/jwks"))
                ) // Habilita OAuth2 Resource Server con JWT
                .build();
    }
}
