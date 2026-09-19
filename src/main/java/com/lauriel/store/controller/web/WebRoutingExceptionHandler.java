package com.lauriel.store.controller.web;

import com.lauriel.store.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

/** Mapping errors can occur before Spring selects a controller, so package selectors cannot handle them. */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebRoutingExceptionHandler {
    private final GlobalExceptionHandler apiErrors;

    public WebRoutingExceptionHandler(GlobalExceptionHandler apiErrors) {
        this.apiErrors = apiErrors;
    }

    @ExceptionHandler({
        org.springframework.web.HttpRequestMethodNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotAcceptableException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class,
        org.springframework.web.servlet.resource.NoResourceFoundException.class
    })
    public Object routing(Exception exception, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return apiErrors.handleException(exception, new ServletWebRequest(request, response));
        }
        response.setContentType("text/html;charset=UTF-8");
        request.removeAttribute(org.springframework.web.servlet.View.SELECTED_CONTENT_TYPE);
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
                java.util.Set.of(org.springframework.http.MediaType.TEXT_HTML));
        var error = (org.springframework.web.ErrorResponse) exception;
        error.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        int status = error.getStatusCode().value();
        var view = new ModelAndView(status == 404 ? "error/404" : "error/400");
        view.setStatus(HttpStatus.valueOf(status));
        view.addObject("message", status == 404 ? "Không tìm thấy trang." : "Yêu cầu này không được hỗ trợ.");
        return view;
    }
}
