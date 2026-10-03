package com.lauriel.store.controller.web;

import com.lauriel.store.dto.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

public class WebPaginationInterceptor implements HandlerInterceptor {
    private static final Set<String> FILTERS = Set.of(
            "keyword", "candidateKeyword", "status", "from", "to", "size", "threshold", "type",
            "variantId", "supplierId", "categoryId");

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView model) {
        if (model == null || !(model.getModel().get("page") instanceof PageResponse<?> page)) {
            return;
        }
        var builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        request.getParameterMap().forEach((name, values) -> {
            if (FILTERS.contains(name)) builder.queryParam(name, (Object[]) values);
        });
        model.addObject("previousPageUrl", url(builder, Math.max(0, page.page() - 1)));
        model.addObject("nextPageUrl", url(builder, page.page() + 1));
    }

    private String url(UriComponentsBuilder builder, int page) {
        // Query-string '+' must be escaped so servlet form decoding preserves a literal plus.
        return builder.cloneBuilder().replaceQueryParam("page", page)
                .build().encode().toUriString().replace("+", "%2B");
    }
}
