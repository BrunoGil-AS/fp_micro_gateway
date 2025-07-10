package com.aspiresys.fp_micro_gateway.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.aspiresys.fp_micro_gateway.config.security.GatewayConstants.*;


/**
 * Spring Security configuration class for the microservices gateway.
 * 
 * This configuration class sets up reactive security for a Spring WebFlux gateway application,
 * providing JWT-based authentication and role-based authorization for various microservice endpoints.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>JWT OAuth2 resource server configuration</li>
 *   <li>Role-based access control (USER and ADMIN roles)</li>
 *   <li>CORS configuration for frontend integration</li>
 *   <li>Endpoint-specific security rules for different microservices</li>
 * </ul>
 * 
 * <p>Protected services include:</p>
 * <ul>
 *   <li><strong>Product Service:</strong> Public read access, admin-only write operations</li>
 *   <li><strong>User Service:</strong> User role required for personal data operations</li>
 *   <li><strong>Order Service:</strong> User role for personal orders, admin role for all orders</li>
 *   <li><strong>Gateway Health:</strong> Role-based health check access</li>
 * </ul>
 * 
 * <p>The JWT authentication converter extracts roles from JWT claims including 'authorities',
 * 'roles', or 'scope' fields and automatically prefixes them with 'ROLE_' for Spring Security
 * compatibility.</p>
 * 
 * <p>CORS is configured to allow requests from the configured frontend URL with credentials
 * support for seamless frontend-backend communication.</p>
 * 
 * @author Bruno Gil
 * @since 1.0
 * @see org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
 * @see org.springframework.security.config.web.server.SecurityWebFilterChain
 */
@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${service.env.frontend.server}")
    private String frontendUrl;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos
                        .pathMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .pathMatchers(PUBLIC_ACTUATOR_ENDPOINTS).permitAll()
                        .pathMatchers(PUBLIC_GATEWAY_HEALTH).permitAll()
                        .pathMatchers(GATEWAY_HEALTH_USER).hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .pathMatchers(GATEWAY_HEALTH_ADMIN).hasRole(ROLE_ADMIN)

                        // PRODUCT SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, PUBLIC_PRODUCT_ENDPOINTS).permitAll()
                        .pathMatchers(HttpMethod.POST, PRODUCT_SERVICE_BASE).hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.PUT, PRODUCT_SERVICE_BASE).hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.DELETE, PRODUCT_SERVICE_BASE).hasRole(ROLE_ADMIN)

                        // USER SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, PUBLIC_USER_HELLO_ENDPOINT).permitAll()
                        .pathMatchers(HttpMethod.GET, USER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.POST, USER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.PUT, USER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.DELETE, USER_SERVICE_ME_BASE).hasRole(ROLE_USER)

                        // ORDER SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, ORDER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.POST, ORDER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.PUT, ORDER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.DELETE, ORDER_SERVICE_ME_BASE).hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.GET, ORDER_SERVICE_ORDERS).hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.GET, ORDER_SERVICE_FIND).hasRole(ROLE_ADMIN)
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Configuration of the JWT authentication converter to extract roles/authorities.
     * Extracts roles from the 'authorities' or 'roles' claim of the JWT.
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> authorities = null;
            if (jwt.hasClaim("authorities")) {
                authorities = jwt.getClaimAsStringList("authorities");
            } else if (jwt.hasClaim("roles")) {
                authorities = jwt.getClaimAsStringList("roles");
            } else if (jwt.hasClaim("scope")) {
                String scope = jwt.getClaimAsString("scope");
                authorities = Arrays.asList(scope.split(" "));
            }
            if (authorities != null) {
                return authorities.stream()
                        .map(authority -> authority.startsWith(ROLE_PREFIX) ? authority : ROLE_PREFIX + authority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            return Arrays.asList();
        });
        
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * CORS configuration to allow access from the frontend.
     * Allows requests from React frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir el origen del frontend
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        // Métodos y headers desde constantes
        configuration.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList(ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
