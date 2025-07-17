package com.aspiresys.fp_micro_gateway.config.security;

import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration for the Gateway.
 *
 * <p>
 * Spring Boot will automatically configure the {@code ReactiveJwtDecoder}
 * when it detects the {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 * property in the configuration file.
 * </p>
 *
 * <p>
 * No additional bean definition is required here unless you need custom JWT decoding logic.
 * </p>
 * 
 */
@Configuration
public class JwtConfig {
}
