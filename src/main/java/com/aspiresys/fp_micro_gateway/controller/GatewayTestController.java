package com.aspiresys.fp_micro_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway")
public class GatewayTestController {

    @GetMapping("/health")
    public String health() {
        return "Gateway is running!";
    }

    @GetMapping("/user-info")
    public String getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            return "User: " + jwt.getSubject() + ", Roles: " + jwt.getClaim("roles");
        }
        return "No authentication information available";
    }
}
