package com.aspiresys.fp_micro_gateway.config;

import com.aspiresys.fp_micro_gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for defining custom gateway routes and filters.
 * <p>
 * This class sets up route mappings for the API Gateway using Spring Cloud Gateway.
 * It defines routes for authentication services and other authenticated microservices,
 * applying a JWT authentication filter to secure API endpoints.
 * </p>
 *
 * <ul>
 *   <li>Routes requests with path "/auth/**" to the authentication server URL specified in the configuration.</li>
 *   <li>Routes requests with path "/api/**" to a load-balanced microservice, applying JWT authentication.</li>
 * </ul>
 *
 * Dependencies:
 * <ul>
 *   <li>{@link JwtAuthenticationFilter} for securing API endpoints.</li>
 *   <li>Spring's {@link RouteLocatorBuilder} for building custom routes.</li>
 * </ul>
 *
 * Configuration Properties:
 * <ul>
 *   <li><b>service.env.auth.server</b>: URL of the authentication server.</li>
 * </ul>
 */
@Configuration
public class GatewayConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${service.env.auth.server}")
        private String authServerUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri(authServerUrl)) 
                
                .route("authenticated-services", r -> r
                        .path("/api/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://MICROSERVICE-NAME")) 
                
                .build();
    }
}
