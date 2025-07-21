package com.aspiresys.fp_micro_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * <p>
 * <strong>JwtAuthenticationFilter</strong> is a custom Spring Cloud Gateway filter that intercepts incoming HTTP requests
 * to validate JWT tokens present in the <code>Authorization</code> header. If a valid Bearer token is found, it decodes
 * the JWT using a <code>ReactiveJwtDecoder</code> and extracts user information such as user ID and roles. These details
 * are then added as custom headers (<code>X-User-Id</code> and <code>X-User-Roles</code>) to the request, making them available
 * to downstream microservices. If the token is invalid, the filter responds with HTTP 401 Unauthorized.
 * If no Authorization header is present, the request proceeds without authentication.
 * </p>
 *
 * <h3>Usage:</h3>
 * <ul>
 *   <li>Place this filter in the Spring Cloud Gateway filter chain to enable JWT-based authentication and
 *   user context propagation.</li>
 * </ul>
 *
 * <h3>Dependencies:</h3>
 * <ul>
 *   <li>Requires a <code>ReactiveJwtDecoder</code> bean to be available in the application context.</li>
 * </ul>
 *
 * <h3>Configuration:</h3>
 * <ul>
 *   <li>The filter can be configured via the nested static <code>Config</code> class if needed.</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private ReactiveJwtDecoder reactiveJwtDecoder;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    /**
     * This method applies the JWT authentication logic to the incoming request.
     * <p> It checks for the presence of an Authorization header,
     * validates the JWT token, and extracts user information to add to the request headers.
     * If the token is invalid, it responds with HTTP 401 Unauthorized. </p>
     * 
     * @param config The configuration for the filter, which can be used to set properties if needed.
     * @return A GatewayFilter that processes the request and applies JWT authentication.
     * <h3>Note:</h3>
     * <p>This method is invoked for each request passing through the gateway.
     * It is responsible for applying the JWT authentication logic and modifying the request as needed.
     * </p>
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Check for Authorization header
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
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

    /**
     * Configuration class for JwtAuthenticationFilter.
     * <p>
     * This class can be used to define additional configuration properties for the filter if needed.
     * Currently, it does not contain any properties but serves as a placeholder for future enhancements.
     * </p>
     * <h3>Usage:</h3>
     * <ul>
     *  <li>Can be used to define custom properties for the filter in the application configuration.</li>
     * </ul>
     * <h3>Example:</h3>
     * <pre>
     * JwtAuthenticationFilter.Config config = new JwtAuthenticationFilter.Config();
     * // Set properties if needed
     * </pre>
     * <p>
     * Note: Currently, this class does not have any configurable properties.
     * </p>
     */
    public static class Config {
        
    }
}
