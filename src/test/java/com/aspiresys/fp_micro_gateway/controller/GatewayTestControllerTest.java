package com.aspiresys.fp_micro_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.aspiresys.fp_micro_gateway.config.SecurityConfig;
import com.aspiresys.fp_micro_gateway.config.TestSecurityConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@WebFluxTest(controllers = GatewayTestController.class)
@Import({GatewayTestController.class, SecurityConfig.class, TestSecurityConfig.class})
class GatewayTestControllerTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testEndpointWithoutAuthentication() {
        webTestClient.get()
            .uri("/gateway/test")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void testEndpointWithUserRole() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user123")
            .claim("roles", List.of("ROLE_USER"))
            .build();

        webTestClient.get()
            .uri("/gateway/test")
            .headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("authenticated")
            .jsonPath("$.user").isEqualTo("user123")
            .jsonPath("$.roles").isArray()
            .jsonPath("$.roles[0]").isEqualTo("ROLE_USER")
            .jsonPath("$.message").isEqualTo("Gateway authentication working correctly");
    }

    @Test
    void testPublicHealthCheck() {
        webTestClient.get()
            .uri("/gateway/health/public")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("Gateway")
            .jsonPath("$.message").isEqualTo("Gateway funcionando correctamente - Acceso público");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminHealthCheck() {
        webTestClient.get()
            .uri("/gateway/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("Gateway")
            .jsonPath("$.message").isEqualTo("Gateway funcionando correctamente - Acceso autorizado para ADMIN");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testUserHealthCheck() {
        webTestClient.get()
            .uri("/gateway/health/user")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("Gateway")
            .jsonPath("$.message").isEqualTo("Gateway funcionando correctamente - Acceso autorizado para USER/ADMIN");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUserHealthCheckWithAdminRole() {
        webTestClient.get()
            .uri("/gateway/health/user")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("Gateway")
            .jsonPath("$.message").isEqualTo("Gateway funcionando correctamente - Acceso autorizado para USER/ADMIN");
    }
}

