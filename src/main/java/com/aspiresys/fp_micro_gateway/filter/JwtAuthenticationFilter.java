package com.aspiresys.fp_micro_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private ReactiveJwtDecoder reactiveJwtDecoder;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            // Si no hay header de autorización, continuar sin autenticación
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7);
            
            return reactiveJwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Agregar información del usuario a los headers para los microservicios downstream
                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(originalRequest -> originalRequest
                            .header("X-User-Id", jwt.getSubject())
                            .header("X-User-Roles", String.join(",", jwt.getClaimAsStringList("roles")))
                        )
                        .build();
                    
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(error -> {
                    // Token inválido
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
        };
    }

    public static class Config {
        // Configuración del filtro si es necesaria
    }
}
