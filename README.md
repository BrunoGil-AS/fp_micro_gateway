# FP Micro Gateway

## Overview

The **FP Micro Gateway** is a Spring Cloud Gateway-based API Gateway that serves as the single entry point for all client requests in the FP (Final Project) microservices ecosystem. It provides centralized routing, authentication, authorization, load balancing, and cross-cutting concerns for the distributed system.

This gateway acts as a reverse proxy, routing requests to appropriate microservices while handling security, CORS, and providing a unified API interface for frontend applications.

## Architecture

```text
┌─────────────────┐
│   Frontend      │
│  (React/Vue)    │
│ :3000           │
└─────────┬───────┘
          │ HTTP/HTTPS
          │
┌─────────▼───────┐
│   API Gateway   │
│   :8080         │
│                 │
├─ Routing        │
├─ Authentication │
├─ Authorization  │
├─ CORS          │
└─────────┬───────┘
          │
          ├─────────────────┬─────────────────┬─────────────────┬
          │                 │                 │                 │
┌─────────▼───────┐ ┌─────────▼───────┐ ┌─────────▼───────┐ ┌─────────▼───────┐
│  Auth Service   │ │  User Service   │ │ Product Service │ │ Order Service   │
│    :8081        │ │    :8082        │ │    :8083        │ │    :8084        │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
          │                 │                 │                 │
          └─────────────────┼─────────────────┼─────────────────┘
                            │                 │
                  ┌─────────▼───────┐ ┌─────────▼───────┐
                  │ Discovery Server│ │ Config Server   │
                  │    :8761        │ │    :8888        │
                  └─────────────────┘ └─────────────────┘
```

## Key Features

- **API Gateway Pattern**: Single entry point for all client requests
- **Dynamic Service Discovery**: Integration with Eureka for automatic service discovery
- **JWT Authentication**: OAuth2 Resource Server with JWT token validation
- **Role-Based Authorization**: Granular access control with USER and ADMIN roles
- **CORS Support**: Cross-Origin Resource Sharing configuration for frontend access
- **Load Balancing**: Automatic load balancing across service instances
- **Request Routing**: Path-based routing to downstream microservices
- **Security Filters**: Custom JWT authentication filters
- **Health Monitoring**: Built-in health check endpoints

## Technology Stack

- **Spring Boot**: 3.3.12
- **Spring Cloud Gateway**: 2023.0.5
- **Spring Security**: OAuth2 Resource Server
- **Spring Cloud Netflix Eureka**: Service Discovery Client
- **Spring Cloud Config**: Centralized configuration management
- **Project Lombok**: Code generation
- **Java**: 17
- **Maven**: Build tool

## Configuration

### Core Application Properties

The main configuration is located in [`application.properties`](src/main/resources/application.properties):

```properties
spring.application.name=fp_micro_gateway

# Config client configuration
spring.config.import=optional:configserver:http://localhost:8888

# Debug logging for gateway
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.cloud=DEBUG
```

### External Configuration

The external configuration is managed via Config Server in [`fp_micro_gateway.properties`](src/main/resources/fp_micro_gateway.properties):

```properties
# Server Configuration
server.port=8080
spring.application.name=gateway-service

# Eureka Client Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true

# Gateway Discovery Configuration
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true

# OAuth2 Resource Server Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081

# Static Route Configuration
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**
```

### Important Configuration Settings

#### 1. Service Discovery

```properties
# Enable automatic route discovery through Eureka
spring.cloud.gateway.discovery.locator.enabled=true
spring.cloud.gateway.discovery.locator.lower-case-service-id=true

# Eureka server connection
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

#### 2. JWT Security

```properties
# OAuth2 Resource Server configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081
```

#### 3. Static Routes

```properties
# Auth service route (bypasses authentication)
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**
```

## Key Components

### 1. Main Application Class

**File**: [`FpMicroGatewayApplication.java`](src/main/java/com/aspiresys/fp_micro_gateway/FpMicroGatewayApplication.java)

```java
@SpringBootApplication
@EnableDiscoveryClient
public class FpMicroGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(FpMicroGatewayApplication.class, args);
    }
}
```

The `@EnableDiscoveryClient` annotation enables Eureka service discovery functionality.

### 2. Gateway Route Configuration

**File**: [`GatewayConfig.java`](src/main/java/com/aspiresys/fp_micro_gateway/config/GatewayConfig.java)

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth service route (no authentication required)
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8081"))

                // Other microservices routes (with JWT authentication)
                .route("authenticated-services", r -> r
                        .path("/api/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://MICROSERVICE-NAME"))

                .build();
    }
}
```

This configuration defines:

- **Authentication bypass** for auth service endpoints (`/auth/**`)
  - bypassing means that these endpoints do not require authentication.
- **JWT authentication filter** for API endpoints (`/api/**`)
  - applies JWT authentication to all requests matching this pattern.
- **Load balancing** using Eureka service discovery (`lb://`)
  - routes requests to registered microservices.

### 3. Security Configuration

**File**: [`SecurityConfig.java`](src/main/java/com/aspiresys/fp_micro_gateway/config/SecurityConfig.java)

````java

This class implements comprehensive security configuration:

#### Path-Based Authorization Rules

```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                    // Public endpoints
                    .pathMatchers("/auth/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/gateway/health/public").permitAll()

                    // Product Service - Public read access
                    .pathMatchers(HttpMethod.GET, "/product-service/products/**").permitAll()

                    // Product Service - Admin only for modifications
                    .pathMatchers(HttpMethod.POST, "/product-service/**").hasRole("ADMIN")
                    .pathMatchers(HttpMethod.PUT, "/product-service/**").hasRole("ADMIN")
                    .pathMatchers(HttpMethod.DELETE, "/product-service/**").hasRole("ADMIN")

                    // User Service - Self-service endpoints
                    .pathMatchers("/user-service/users/me/**").hasRole("USER")

                    // Order Service - User access to own orders
                    .pathMatchers("/order-service/orders/me/**").hasRole("USER")
                    .pathMatchers(HttpMethod.GET, "/order-service/orders").hasRole("ADMIN")

                    // Any other request requires authentication
                    .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
}
````

This configuration:

- **Disables CSRF (Cross-Site Request Forgery) protection** (not needed for stateless APIs)
- **Enables CORS** for frontend applications defined in [`corsConfigurationSource()`](#cors-configuration)
- **Defines authorization rules** for different service endpoints
- **Configures JWT authentication** for protected routes defined in [`jwtAuthenticationConverter()`](#jwt-authentication-converter)

#### JWT Authentication Converter

```java
@Bean
public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<String> authorities = null;

        // Try to get authorities from different JWT claims
        if (jwt.hasClaim("authorities")) {
            authorities = jwt.getClaimAsStringList("authorities");
        } else if (jwt.hasClaim("roles")) {
            authorities = jwt.getClaimAsStringList("roles");
        } else if (jwt.hasClaim("scope")) {
            String scope = jwt.getClaimAsString("scope");
            authorities = Arrays.asList(scope.split(" "));
        }

        // Convert to SimpleGrantedAuthority with ROLE_ prefix
        if (authorities != null) {
            return authorities.stream()
                    .map(authority -> authority.startsWith("ROLE_") ? authority : "ROLE_" + authority)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return Arrays.asList();
    });

    return new ReactiveJwtAuthenticationConverterAdapter(converter);
}
```

This converter:

- **Extracts** user roles from JWT claims
- **Converts** roles to `SimpleGrantedAuthority` with `ROLE_` prefix
- **Ensures** proper role-based access control in the gateway
- **Returns** a `ReactiveJwtAuthenticationConverterAdapter` for use in the security filter chain

#### CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow frontend origin
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

    // Allow all necessary HTTP methods
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Allow all headers
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // Allow credentials
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
}
```

This configuration:

- **Disables CSRF protection** (not needed for stateless APIs)
- **Enables CORS** for frontend applications
- **Defines authorization rules** for different service endpoints
- **Configures JWT authentication** for protected routes
- **Returns** a `CorsConfigurationSource` bean for CORS handling

### 4. JWT Authentication Filter

**File**: [`JwtAuthenticationFilter.java`](src/main/java/com/aspiresys/fp_micro_gateway/filter/JwtAuthenticationFilter.java)

```java
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Skip if no Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7);

            return reactiveJwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // Add user information to headers for downstream services
                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(originalRequest -> originalRequest
                            .header("X-User-Id", jwt.getSubject())
                            .header("X-User-Roles", String.join(",", jwt.getClaimAsStringList("roles")))
                        )
                        .build();

                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(error -> {
                    // Invalid token
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
        };
    }
}
```

This filter:

- **Validates JWT tokens** for protected routes
- **Adds user context** to downstream service requests
- **Handles authentication errors** gracefully

### 5. Gateway Test Controller

**File**: [`GatewayTestController.java`](src/main/java/com/aspiresys/fp_micro_gateway/controller/GatewayTestController.java)

Provides health check and testing endpoints:

```java
@RestController
@RequestMapping("/gateway")
public class GatewayTestController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/test")
    public Mono<Map<String, Object>> test(@AuthenticationPrincipal Jwt jwt) {
        return Mono.just(Map.of(
            "status", "authenticated",
            "user", jwt.getSubject(),
            "roles", jwt.getClaimAsStringList("roles"),
            "message", "Gateway authentication working correctly"
        ));
    }

    @GetMapping("/health/public")
    public Mono<ResponseEntity<Map<String, Object>>> publicHealthCheck() {
        // Public health check endpoint
    }

    @GetMapping("/health/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> userHealthCheck() {
        // User-level health check endpoint
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> adminHealthCheck() {
        // Admin-only health check endpoint
    }
}
```

This controller provides:

- **Test endpoint** for JWT authentication
- **Public health check** endpoint
- **User-level health check** endpoint
- **Admin health check** endpoint

## API Endpoints and Routing

### Gateway Endpoints

| Method | Endpoint                 | Access Level | Description             |
| ------ | ------------------------ | ------------ | ----------------------- |
| GET    | `/gateway/test`          | USER         | Test JWT authentication |
| GET    | `/gateway/health/public` | Public       | Public health check     |
| GET    | `/gateway/health/user`   | USER/ADMIN   | User-level health check |
| GET    | `/gateway/health`        | ADMIN        | Admin health check      |

### Routing Rules

#### Authentication Service (Public Access)

```text
/auth/** → http://localhost:8081
```

- **OAuth2 Authorization**: `GET /auth/oauth2/authorize`
- **Token Exchange**: `POST /auth/oauth2/token`

#### Product Service

| Method | Pattern                        | Access | Target Service  |
| ------ | ------------------------------ | ------ | --------------- |
| GET    | `/product-service/products/**` | Public | Product Service |
| POST   | `/product-service/**`          | ADMIN  | Product Service |
| PUT    | `/product-service/**`          | ADMIN  | Product Service |
| DELETE | `/product-service/**`          | ADMIN  | Product Service |

#### User Service

| Method | Pattern                     | Access | Target Service |
| ------ | --------------------------- | ------ | -------------- |
| GET    | `/user-service/users/hello` | Public | User Service   |
| \*     | `/user-service/users/me/**` | USER   | User Service   |

#### Order Service

| Method | Pattern                       | Access | Target Service |
| ------ | ----------------------------- | ------ | -------------- |
| \*     | `/order-service/orders/me/**` | USER   | Order Service  |
| GET    | `/order-service/orders`       | ADMIN  | Order Service  |
| GET    | `/order-service/orders/find`  | ADMIN  | Order Service  |

### Example API Calls

#### 1. Get JWT Token

```bash
# Get authorization code
curl -X GET "http://localhost:8081/oauth2/authorize?response_type=code&client_id=frontend-client&redirect_uri=http://localhost:3000/callback&scope=read"

# Exchange code for token
curl -X POST "http://localhost:8081/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&code=CODE&redirect_uri=http://localhost:3000/callback&client_id=frontend-client"
```

This process involves:

- **Code exchange** frontend application sending a code challenge to exchange for an authorization code
- **Token exchange** to get the JWT token

#### 2. Access Protected Endpoints

```bash
# Test gateway authentication
curl -X GET "http://localhost:8080/gateway/test" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get products (public access)
curl -X GET "http://localhost:8080/product-service/products"

# Create product (admin only)
curl -X POST "http://localhost:8080/product-service/products" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "New Product", "price": 29.99}'

# Get user orders (user access)
curl -X GET "http://localhost:8080/order-service/orders/me" \
  -H "Authorization: Bearer USER_JWT_TOKEN"
```

## Running the Application

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Running Config Server (`fp_micro_configserver` on port 8888)
- Running Discovery Server (`fp_micro_discoveryserver` on port 8761)
- Running Auth Service (`fp_micro_authservice` on port 8081)

### Development Mode

```bash
# Clone the repository
git clone <repository-url>
cd fp_micro_gateway

# Start the gateway
./mvnw spring-boot:run

# Or run the JAR file
./mvnw clean package
java -jar target/fp_micro_gateway-0.0.1-SNAPSHOT.war
```

### Startup Sequence

1. **Config Server** (port 8888)
2. **Discovery Server** (port 8761)
3. **Auth Service** (port 8081)
4. **Gateway** (port 8080)
5. **Other microservices** (ports 9001+):
   - User Service (port 9001)
   - Product Service (port 9002)
   - Order Service (port 9003)

### Verification

Once started, verify the gateway is working:

```bash
# Check public health endpoint
curl http://localhost:8080/gateway/health/public

# Check service registration in Eureka
curl http://localhost:8761/eureka/apps

# Check gateway routes
curl http://localhost:8080/actuator/gateway/routes
```

## Security Architecture

### Authentication Flow

```text
1. Client → Gateway: Request with JWT token
2. Gateway → Auth Service: Validate JWT (if needed)
3. Gateway: Extract user roles from JWT
4. Gateway: Check authorization rules
5. Gateway → Microservice: Forward request with user context
6. Microservice → Gateway: Response
7. Gateway → Client: Final response
```

### Authorization Levels

#### Public Access

- Product catalog viewing
- Auth service endpoints
- Health check endpoints

#### USER Role

- User profile management (`/user-service/users/me/**`)
- Order management (`/order-service/orders/me/**`)
- Gateway test endpoints

#### ADMIN Role

- Product management (CREATE, UPDATE, DELETE)
- Order administration
- System health monitoring
- User management

### Security Headers

The gateway automatically adds security headers to downstream requests:

```text
X-User-Id: user-uuid-from-jwt
X-User-Roles: ROLE_USER,ROLE_ADMIN
```

## Monitoring and Troubleshooting

### Health Monitoring

```bash
# Gateway public health
curl http://localhost:8080/gateway/health/public

# Spring Boot Actuator health
curl http://localhost:8080/actuator/health

# Gateway-specific metrics
curl http://localhost:8080/actuator/gateway/routes
curl http://localhost:8080/actuator/gateway/globalfilters
```

### Log Configuration

The gateway uses a comprehensive Logback configuration located in `src/main/resources/logback-spring.xml` that provides structured logging across different components:

#### Log File Structure

```text
logs/gateway/
├── gateway.log                    # General gateway logs
├── gateway-routing.log            # Route-specific logs
├── gateway-security.log           # Security and JWT logs
├── gateway-error.log              # Error logs (WARN and above)
└── archived/                      # Compressed archived logs
    ├── gateway.2025-01-15.1.log.gz
    ├── gateway-routing.2025-01-15.1.log.gz
    └── ...
```

#### Logging Configuration by Component

**Gateway Application Logs**:

```xml
<logger name="com.aspiresys.fp_micro_gateway" level="DEBUG">
    <appender-ref ref="FILE_GATEWAY"/>
    <appender-ref ref="FILE_ERROR"/>
    <appender-ref ref="CONSOLE"/> <!-- Development only -->
</logger>
```

**Spring Cloud Gateway Routing**:

```xml
<logger name="org.springframework.cloud.gateway" level="INFO">
    <appender-ref ref="FILE_ROUTING"/>
    <appender-ref ref="FILE_ERROR"/>
</logger>

<logger name="org.springframework.cloud.gateway.filter" level="DEBUG">
    <appender-ref ref="FILE_ROUTING"/>
    <appender-ref ref="FILE_ERROR"/>
</logger>
```

**Security and JWT Logs**:

```xml
<logger name="org.springframework.security.oauth2" level="INFO">
    <appender-ref ref="FILE_SECURITY"/>
    <appender-ref ref="FILE_ERROR"/>
</logger>

<logger name="org.springframework.security.oauth2.jwt" level="DEBUG">
    <appender-ref ref="FILE_SECURITY"/>
    <appender-ref ref="FILE_ERROR"/>
</logger>
```

#### Log Rotation Configuration

- **Gateway Logs**: 50MB max file size, 30 days retention, 800MB total cap
- **Routing Logs**: 40MB max file size, 30 days retention, 600MB total cap
- **Security Logs**: 30MB max file size, 45 days retention, 500MB total cap
- **Error Logs**: 25MB max file size, 60 days retention, 500MB total cap

#### Profile-Based Logging

**Development Profile** (`!prod`): Logs to both console and files
**Production Profile** (`prod`): Logs only to files

#### Additional Debug Configuration

For enhanced debugging in `application.properties`:

```properties
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.cloud=DEBUG
```

### Common Issues and Solutions

#### 1. Service Discovery Issues

```bash
# Check Eureka registration
curl http://localhost:8761/eureka/apps

# Verify service names match in configuration
```

#### 2. JWT Authentication Problems

```bash
# Verify Auth Service is running
curl http://localhost:8081/actuator/health

# Check JWT issuer configuration
# Ensure: spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081
```

#### 3. CORS Issues

```bash
# Verify allowed origins in SecurityConfig
# Default: http://localhost:3000

# Check preflight OPTIONS requests
curl -X OPTIONS http://localhost:8080/product-service/products \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"
```

#### 4. Route Not Found

```bash
# Check active routes
curl http://localhost:8080/actuator/gateway/routes

# Verify service is registered in Eureka
curl http://localhost:8761/eureka/apps
```

### Testing

Run the test suite:

```bash
./mvnw test
```

Key test classes:

- `GatewayTestControllerTest`: Tests the gateway endpoints
- Integration tests for security configuration

## Integration with Other Services

### Service Registration

Services register with Eureka and are automatically discoverable:

```properties
# In microservice configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
spring.application.name=user-service
```

### Gateway Discovery

Gateway automatically creates routes for registered services:

```text
http://localhost:8080/user-service/** → lb://user-service
http://localhost:8080/product-service/** → lb://product-service
```

### Configuration Management

Gateway configuration is managed centrally:

```properties
# Retrieved from Config Server
spring.config.import=optional:configserver:http://localhost:8888
```

## Best Practices

### Security

1. **JWT Validation**: Always validate JWT tokens for protected endpoints
2. **Role-Based Access**: Implement granular permissions based on user roles
3. **HTTPS**: Use HTTPS in production environments
4. **Token Expiration**: Implement proper token refresh mechanisms

### Performance

1. **Route Caching**: Leverage Spring Cloud Gateway's built-in route caching
2. **Connection Pooling**: Configure appropriate connection pools for downstream services
3. **Timeout Configuration**: Set appropriate timeouts for service calls
4. **Circuit Breaker**: Implement circuit breaker patterns for resilience

### Monitoring

1. **Health Checks**: Implement comprehensive health check endpoints
2. **Metrics**: Monitor gateway-specific metrics (response times, error rates)
3. **Logging**: Implement structured logging for troubleshooting
4. **Tracing**: Use distributed tracing for request flow visibility

## Production Considerations

### Environment Configuration

```properties
# Production OAuth2 configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://auth.yourdomain.com

# HTTPS configuration
server.ssl.enabled=true
server.ssl.key-store=classpath:gateway.p12
server.ssl.key-store-password=password

# Production CORS origins
cors.allowed-origins=https://yourdomain.com,https://app.yourdomain.com
```

### Scalability

1. **Load Balancing**: Deploy multiple gateway instances behind a load balancer
2. **Database Connection Pooling**: Configure optimal connection pools
3. **JVM Tuning**: Optimize JVM settings for your workload
4. **Resource Limits**: Set appropriate CPU and memory limits

## Contributing

1. Follow the existing code structure and naming conventions
2. Add tests for new functionality
3. Update documentation for configuration changes
4. Ensure security best practices are maintained
5. Test with all dependent services

## References

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [Spring Cloud Netflix Documentation](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
- [Spring Boot Actuator Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
