package com.lauriel.store.security;

import com.lauriel.store.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class WebAccountStateFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public WebAccountStateFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();
        if (
            authentication != null &&
            authentication.isAuthenticated() &&
            authentication.getPrincipal() instanceof StoreUserDetails principal
        ) {
            var current = users
                .findById(principal.getUserId())
                .map(StoreUserDetails::new)
                .orElse(null);
            if (
                current == null ||
                !current.isEnabled() ||
                !current.isAccountNonLocked()
            ) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendRedirect(
                    request.getContextPath() + "/login?disabled"
                );
                return;
            }
            if (current.getRole() != principal.getRole()) {
                current.eraseCredentials();
                var refreshed =
                    UsernamePasswordAuthenticationToken.authenticated(
                        current,
                        null,
                        current.getAuthorities()
                    );
                refreshed.setDetails(authentication.getDetails());
                context.setAuthentication(refreshed);
            }
        }
        filterChain.doFilter(request, response);
    }
}
