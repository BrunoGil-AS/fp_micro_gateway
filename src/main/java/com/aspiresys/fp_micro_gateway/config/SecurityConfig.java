package com.aspiresys.fp_micro_gateway.config;

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

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Habilita CORS
                .authorizeExchange(exchanges -> exchanges
                        // Permitir acceso público a endpoints del auth service
                        .pathMatchers("/auth/**").permitAll()
                        // Permitir acceso a endpoints de actuator para health checks
                        .pathMatchers("/actuator/**").permitAll()
                        // Permitir acceso público al health check público
                        .pathMatchers("/gateway/health/public").permitAll()
                        // Permitir acceso a health check solo a ADMIN
                        .pathMatchers("/gateway/health").hasRole("ADMIN")
                        .pathMatchers("/gateway/health/user").hasAnyRole("USER", "ADMIN")
                        // Permitir solo a Admin publicar, editar y eliminar productos en el product service
                        .pathMatchers(HttpMethod.POST, "/product-service/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/product-service/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/product-service/**").hasRole("ADMIN")
                        // Permitir acceso público a la consulta de productos
                        .pathMatchers(HttpMethod.GET, "/product-service/products").permitAll()
                        // Configuración específica para user-service
                        .pathMatchers(HttpMethod.GET, "/user-service/users/hello").permitAll() // Endpoint público
                        .pathMatchers(HttpMethod.GET, "/user-service/users/userbody").hasAnyRole("USER", "ADMIN") // Requiere autenticación
                        // Permitir solo a USER manipular sus propias órdenes y datos de usuario
                        .pathMatchers(HttpMethod.GET, "/order-service/orders/me/**").hasRole("USER") //TODO: set up /me in each service.
                        .pathMatchers(HttpMethod.POST, "/order-service/orders/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.PUT, "/order-service/orders/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.DELETE, "/order-service/orders/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/user-service/users/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.POST, "/user-service/users/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.PUT, "/user-service/users/me/**").hasRole("USER")
                        .pathMatchers(HttpMethod.DELETE, "/user-service/users/me/**").hasRole("USER")
                        // Cualquier otra petición requiere autenticación
                        .anyExchange().authenticated()
                )
                // Configurar como servidor de recursos OAuth2 con JWT
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
            // Intentar obtener authorities desde diferentes claims posibles
            Collection<String> authorities = null;
            
            // Primero intentar con 'authorities'
            if (jwt.hasClaim("authorities")) {
                authorities = jwt.getClaimAsStringList("authorities");
            }
            // Si no existe, intentar con 'roles'
            else if (jwt.hasClaim("roles")) {
                authorities = jwt.getClaimAsStringList("roles");
            }
            // Si no existe, intentar con 'scope' (separado por espacios)
            else if (jwt.hasClaim("scope")) {
                String scope = jwt.getClaimAsString("scope");
                authorities = Arrays.asList(scope.split(" "));
            }
            
            // Convertir a SimpleGrantedAuthority y asegurar prefijo ROLE_
            if (authorities != null) {
                return authorities.stream()
                        .map(authority -> authority.startsWith("ROLE_") ? authority : "ROLE_" + authority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            
            return Arrays.asList(); // Retornar lista vacía si no hay authorities
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
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // Permitir todos los métodos HTTP necesarios
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Permitir todos los headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Permitir cookies y credenciales
        configuration.setAllowCredentials(true);
        
        // Configurar para todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
