package com.lauriel.store.config;

import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.security.ApiSecurityErrorHandler;
import com.lauriel.store.security.DatabaseJwtAuthenticationConverter;
import com.lauriel.store.security.RoleAwareAuthenticationSuccessHandler;
import com.lauriel.store.security.WebAccountStateFilter;
import com.lauriel.store.service.StoreCartService;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Bean
    public RoleAwareAuthenticationSuccessHandler roleAwareAuthenticationSuccessHandler(
        org.springframework.core.env.Environment environment,
        org.springframework.beans.factory.ObjectProvider<StoreCartService> carts
    ) {
        return new RoleAwareAuthenticationSuccessHandler(
            environment.getProperty("app.admin.port", Integer.class, 8080),
            environment.getProperty("app.storefront.port", Integer.class, 8081),
            carts.getIfAvailable()
        );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(
        HttpSecurity http,
        DatabaseJwtAuthenticationConverter converter,
        ApiSecurityErrorHandler errors
    ) throws Exception {
        return http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable()) // Only this stateless Bearer chain disables CSRF.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login"
                    )
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/promotions/active",
                        "/api/order-promotions/active",
                        "/api/vouchers/active"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/vouchers/validate")
                    .hasRole("CUSTOMER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/products",
                        "/api/products/{id}",
                        "/api/products/{productId}/variants",
                        "/api/products/{id}/related",
                        "/api/sizes",
                        "/api/colors",
                        "/api/categories"
                    )
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/admin/categories",
                        "/api/admin/categories/{id}"
                    )
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/admin/products",
                        "/api/admin/products/{productId}/variants",
                        "/api/admin/sizes",
                        "/api/admin/colors",
                        "/api/admin/categories"
                    )
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers(
                        HttpMethod.PUT,
                        "/api/admin/products/{id}",
                        "/api/admin/product-variants/{variantId}",
                        "/api/admin/sizes/{id}",
                        "/api/admin/colors/{id}",
                        "/api/admin/categories/{id}"
                    )
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/admin/products/{id}/status",
                        "/api/admin/categories/{id}/status"
                    )
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                    .requestMatchers("/api/staff/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers("/api/orders", "/api/orders/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers("/api/customer/**", "/api/customers/**")
                    .hasRole("CUSTOMER")
                    .anyRequest()
                    .authenticated()
            )
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(errors)
                    .accessDeniedHandler(errors)
            )
            .oauth2ResourceServer(resource ->
                resource
                    .authenticationEntryPoint(errors)
                    .accessDeniedHandler(errors)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
            )
            .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurity(
        HttpSecurity http,
        DaoAuthenticationProvider provider,
        RoleAwareAuthenticationSuccessHandler successHandler,
        UserRepository users
    ) throws Exception {
        return http
            .authenticationProvider(provider)
            .authorizeHttpRequests(auth ->
                auth
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/",
                        "/login",
                        "/register",
                        "/error",
                        "/403",
                        "/assets/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/uploads/products/**")
                    .permitAll()
                    .requestMatchers(
                        "/shop/checkout/**",
                        "/shop/orders/**",
                        "/shop/profile/**"
                    )
                    .hasRole("CUSTOMER")
                    .requestMatchers(
                        "/shop",
                        "/shop/products/**",
                        "/shop/cart/**",
                        "/shop/offers/**"
                    )
                    .permitAll()
                    .requestMatchers("/admin/accounts", "/admin/accounts/**")
                    .hasRole("SUPER_ADMIN")
                    .requestMatchers(
                        "/admin",
                        "/admin/dashboard",
                        "/admin/statistics",
                        "/admin/statistics/**",
                        "/admin/products",
                        "/admin/products/**",
                        "/admin/categories",
                        "/admin/categories/**",
                        "/admin/sizes",
                        "/admin/sizes/**",
                        "/admin/colors",
                        "/admin/colors/**",
                        "/admin/inventory",
                        "/admin/inventory/**",
                        "/admin/suppliers",
                        "/admin/suppliers/**",
                        "/admin/purchase-receipts",
                        "/admin/purchase-receipts/**",
                        "/admin/orders",
                        "/admin/orders/**",
                        "/admin/customers"
                    )
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .requestMatchers("/admin/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                    .requestMatchers("/staff/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                    .anyRequest()
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .formLogin(form ->
                form
                    .loginPage("/login")
                    .successHandler(successHandler)
                    .failureUrl("/login?error")
                    .permitAll()
            )
            .exceptionHandling(errors ->
                errors
                    .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login?checkout"),
                        PathPatternRequestMatcher.pathPattern(
                            "/shop/checkout/**"
                        )
                    )
                    .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        AnyRequestMatcher.INSTANCE
                    )
                    .accessDeniedHandler((request, response, exception) -> {
                        response.setStatus(403);
                        request
                            .getRequestDispatcher("/403")
                            .forward(request, response);
                    })
            )
            .logout(logout ->
                logout
                    .addLogoutHandler((request, response, authentication) -> {
                        var session = request.getSession(false);
                        if (session != null) {
                            if (
                                StoreCartService.retainCartOnly(session)
                            ) request.changeSessionId();
                            else session.invalidate();
                        }
                    })
                    .invalidateHttpSession(false)
                    .clearAuthentication(true)
                    .logoutSuccessUrl("/login?logout")
            )
            .addFilterBefore(
                new WebAccountStateFilter(users),
                AuthorizationFilter.class
            )
            .build(); // CSRF remains enabled for session/form login.
    }
}
