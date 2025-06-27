package com.aspiresys.fp_micro_gateway.config;

import com.aspiresys.fp_micro_gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Ruta para el servicio de autenticación (sin autenticación)
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8081"))
                
                // Ruta para otros microservicios (con autenticación JWT)
                .route("authenticated-services", r -> r
                        .path("/api/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://MICROSERVICE-NAME")) // Se resolverá dinámicamente vía Eureka
                
                .build();
    }
}
