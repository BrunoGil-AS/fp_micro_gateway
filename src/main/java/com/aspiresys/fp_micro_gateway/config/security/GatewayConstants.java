package com.aspiresys.fp_micro_gateway.config.security;


/**
 * <h1>GatewayConstants</h1>
 * <p>
 * Utility class containing constant values used in the microservices gateway configuration.
 * Defines allowed HTTP methods, headers, endpoint patterns, and role definitions for security and routing.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>HTTP methods and headers configuration for CORS</li>
 *   <li>Public and protected endpoint patterns for different microservices</li>
 *   <li>Role definitions for authorization</li>
 * </ul>
 *
 * @author Bruno Gil
 * @version 1.0
 * @since 1.0
 */
public final class GatewayConstants {
    private GatewayConstants() {}

    /**
     * Allowed HTTP methods for CORS.
     */
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    /**
     * Allowed headers for CORS
     */
    public static final String[] ALLOWED_HEADERS = {"*"};

    // === ENDPOINTS ===
    // Auth
    public static final String[] PUBLIC_AUTH_ENDPOINTS = {"/auth/**"};
    // Actuator
    public static final String[] PUBLIC_ACTUATOR_ENDPOINTS = {"/actuator/**"};
    // Gateway health
    public static final String[] PUBLIC_GATEWAY_HEALTH = {"/gateway/health/public"};
    public static final String GATEWAY_HEALTH_USER = "/gateway/health/user";
    public static final String GATEWAY_HEALTH_ADMIN = "/gateway/health";

    // Product Service
    public static final String[] PUBLIC_PRODUCT_ENDPOINTS = {"/product-service/products/**", "/product-service/products"};
    public static final String PRODUCT_SERVICE_BASE = "/product-service/**";

    // User Service
    public static final String[] PUBLIC_USER_HELLO_ENDPOINT = {"/user-service/users/hello"};
    public static final String USER_SERVICE_ME_BASE = "/user-service/users/me/**";

    // Order Service
    public static final String ORDER_SERVICE_ME_BASE = "/order-service/orders/me/**";
    public static final String ORDER_SERVICE_ORDERS = "/order-service/orders";
    public static final String ORDER_SERVICE_FIND = "/order-service/orders/find";

    // === ROLES ===
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PREFIX = "ROLE_";
}
