package com.lauriel.store.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

public class RoleAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final int adminPort;
    private final int storefrontPort;

    public RoleAwareAuthenticationSuccessHandler(int adminPort, int storefrontPort) {
        this.adminPort = adminPort;
        this.storefrontPort = storefrontPort;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        boolean staff = authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_STAFF"));
        if (staff) {
            requestCache.removeRequest(request, response);
            response.sendRedirect(target(request, adminPort, "/admin"));
            return;
        }
        var saved = requestCache.getRequest(request, response);
        String target = "/shop";
        if (saved != null) {
            try {
                var uri = java.net.URI.create(saved.getRedirectUrl());
                String path = uri.getPath();
                if (path != null && (path.equals("/shop/checkout") || path.startsWith("/shop/orders")
                        || path.startsWith("/shop/profile"))) {
                    target = path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                }
            } catch (IllegalArgumentException ignored) {
                target = "/shop";
            }
            requestCache.removeRequest(request, response);
        }
        response.sendRedirect(target(request, storefrontPort, target));
    }

    private String target(HttpServletRequest request, int port, String path) {
        if (request.getLocalPort() != adminPort && request.getLocalPort() != storefrontPort) {
            return request.getContextPath() + path;
        }
        return request.getScheme() + "://" + request.getServerName() + ":" + port
                + request.getContextPath() + path;
    }
}
