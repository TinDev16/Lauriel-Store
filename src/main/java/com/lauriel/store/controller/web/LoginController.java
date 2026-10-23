package com.lauriel.store.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final int adminPort;
    private final int storefrontPort;

    public LoginController(
        @Value("${app.admin.port:8080}") int adminPort,
        @Value("${app.storefront.port:8081}") int storefrontPort
    ) {
        this.adminPort = adminPort;
        this.storefrontPort = storefrontPort;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String root(HttpServletRequest request) {
        return request.getLocalPort() == adminPort
            ? "redirect:/admin"
            : "redirect:/shop";
    }
}
