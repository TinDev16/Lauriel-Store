package com.lauriel.store.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortRoutingFilter extends OncePerRequestFilter {
    private final int adminPort;
    private final int storefrontPort;

    public PortRoutingFilter(@Value("${app.admin.port:8080}") int adminPort,
            @Value("${app.storefront.port:8081}") int storefrontPort) {
        this.adminPort = adminPort;
        this.storefrontPort = storefrontPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        int localPort = request.getLocalPort();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (adminPort == storefrontPort) {
            filterChain.doFilter(request, response);
            return;
        }

        if (localPort == adminPort && isStorefrontPath(path)) {
            response.sendRedirect(urlFor(request, storefrontPort));
            return;
        }
        if (localPort == storefrontPort && isBackOfficePath(path)) {
            response.sendRedirect(urlFor(request, adminPort));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isStorefrontPath(String path) {
        return path.equals("/shop") || path.startsWith("/shop/") || path.equals("/register");
    }

    private boolean isBackOfficePath(String path) {
        return path.equals("/admin") || path.startsWith("/admin/")
                || path.equals("/staff") || path.startsWith("/staff/");
    }

    private String urlFor(HttpServletRequest request, int port) {
        StringBuilder target = new StringBuilder(request.getScheme())
                .append("://").append(request.getServerName()).append(':').append(port)
                .append(request.getRequestURI());
        if (request.getQueryString() != null) {
            target.append('?').append(request.getQueryString());
        }
        return target.toString();
    }
}
