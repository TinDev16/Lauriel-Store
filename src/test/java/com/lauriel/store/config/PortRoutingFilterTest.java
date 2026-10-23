package com.lauriel.store.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PortRoutingFilterTest {

    private final PortRoutingFilter filter = new PortRoutingFilter(8080, 8081);

    @Test
    void redirectsStorefrontFromAdminPortAndKeepsQueryString()
        throws Exception {
        MockHttpServletRequest request = request(
            8080,
            "/shop/products",
            "categoryId=3"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, unusedChain());

        assertThat(response.getRedirectedUrl()).isEqualTo(
            "http://localhost:8081/shop/products?categoryId=3"
        );
    }

    @Test
    void redirectsAdminFromStorefrontPort() throws Exception {
        MockHttpServletRequest request = request(8081, "/admin/products", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, unusedChain());

        assertThat(response.getRedirectedUrl()).isEqualTo(
            "http://localhost:8080/admin/products"
        );
    }

    @Test
    void allowsEachAreaOnItsOwnPort() throws Exception {
        AtomicBoolean adminContinued = new AtomicBoolean();
        AtomicBoolean storeContinued = new AtomicBoolean();

        filter.doFilter(
            request(8080, "/admin", null),
            new MockHttpServletResponse(),
            (request, response) -> adminContinued.set(true)
        );
        filter.doFilter(
            request(8081, "/shop", null),
            new MockHttpServletResponse(),
            (request, response) -> storeContinued.set(true)
        );

        assertThat(adminContinued).isTrue();
        assertThat(storeContinued).isTrue();
    }

    private MockHttpServletRequest request(
        int port,
        String path,
        String query
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest(
            "GET",
            path
        );
        request.setScheme("http");
        request.setServerName("localhost");
        request.setLocalPort(port);
        request.setQueryString(query);
        return request;
    }

    private FilterChain unusedChain() {
        return (request, response) -> {
            throw new AssertionError(
                "Không được tiếp tục chuỗi filter sau khi chuyển cổng"
            );
        };
    }
}
