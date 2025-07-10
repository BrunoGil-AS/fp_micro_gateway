package com.aspiresys.fp_micro_gateway.config.security;

/**
 * Constantes usadas en la configuración de seguridad y gateway.
 */
public final class GatewayConstants {
    private GatewayConstants() {}

    // Métodos HTTP permitidos
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    // Headers permitidos
    public static final String[] ALLOWED_HEADERS = {"*"};

    // Rutas públicas
    public static final String[] PUBLIC_AUTH_ENDPOINTS = {"/auth/**"};
    public static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {"/actuator/**"};
    public static final String[] PUBLIC_PRODUCT_ENDPOINTS = {"/product-service/products/**", "/product-service/products"};
    public static final String[] PUBLIC_USER_HELLO_ENDPOINT = {"/user-service/users/hello"};
    public static final String[] PUBLIC_GATEWAY_HEALTH = {"/gateway/health/public"};

    // Roles
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PREFIX = "ROLE_";
}
