package com.aspiresys.fp_micro_gateway.config.security;

import org.springframework.context.annotation.Configuration;

/**
 * Configuración JWT para el Gateway.
 * 
 * Spring Boot configurará automáticamente el ReactiveJwtDecoder cuando detecte
 * la propiedad spring.security.oauth2.resourceserver.jwt.issuer-uri
 * en el archivo de configuración.
 */
@Configuration
public class JwtConfig {
    // Spring Boot auto-configuración se encarga de crear el ReactiveJwtDecoder
}
