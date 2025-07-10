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
                        .pathMatchers("/gateway/health/user").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .pathMatchers("/gateway/health").hasRole(ROLE_ADMIN)

                        // PRODUCT SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, PUBLIC_PRODUCT_ENDPOINTS).permitAll()
                        .pathMatchers(HttpMethod.POST, "/product-service/**").hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.PUT, "/product-service/**").hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.DELETE, "/product-service/**").hasRole(ROLE_ADMIN)

                        // USER SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, PUBLIC_USER_HELLO_ENDPOINT).permitAll()
                        .pathMatchers(HttpMethod.GET, "/user-service/users/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.POST, "/user-service/users/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.PUT, "/user-service/users/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.DELETE, "/user-service/users/me/**").hasRole(ROLE_USER)

                        // ORDER SERVICE ENDPOINTS
                        .pathMatchers(HttpMethod.GET, "/order-service/orders/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.POST, "/order-service/orders/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.PUT, "/order-service/orders/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.DELETE, "/order-service/orders/me/**").hasRole(ROLE_USER)
                        .pathMatchers(HttpMethod.GET, "/order-service/orders").hasRole(ROLE_ADMIN)
                        .pathMatchers(HttpMethod.GET, "/order-service/orders/find").hasRole(ROLE_ADMIN)

                        // Cualquier otra petición requiere autenticación
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Configuración del convertidor de autenticación JWT para extraer roles/authorities.
     * Extrae los roles del claim 'authorities' o 'roles' del JWT.
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
     * Configuración CORS para permitir el acceso desde el frontend.
     * Permite requests desde http://localhost:3000 (React frontend).
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
