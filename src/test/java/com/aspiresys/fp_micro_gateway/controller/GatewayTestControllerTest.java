package com.aspiresys.fp_micro_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.aspiresys.fp_micro_gateway.config.TestSecurityConfig;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebFluxTest(controllers = GatewayTestController.class)
@Import({GatewayTestController.class, TestSecurityConfig.class})
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.config.import="
})
class GatewayTestControllerTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
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
        webTestClient
            .mutateWith(SecurityMockServerConfigurers.mockJwt()
                .jwt(jwt -> jwt
                    .subject("user123")
                    .claim("roles", java.util.List.of("ROLE_USER"))
                )
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            )
            .get()
            .uri("/gateway/test")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("authenticated")
            .jsonPath("$.user").isEqualTo("user123")
            .jsonPath("$.roles").isArray()
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

