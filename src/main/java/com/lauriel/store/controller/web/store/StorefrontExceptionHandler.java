package com.lauriel.store.controller.web.store;

import com.lauriel.store.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = "com.lauriel.store.controller.web.store")
@org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
public class StorefrontExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ModelAndView missing(ResourceNotFoundException exception) {
        return page(HttpStatus.NOT_FOUND, "Không tìm thấy nội dung", exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, ResourceConflictException.class, DuplicateResourceException.class})
    ModelAndView business(RuntimeException exception) {
        return page(HttpStatus.BAD_REQUEST, "Không thể hoàn tất yêu cầu", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ModelAndView failure(Exception exception, HttpServletRequest request) {
        LoggerFactory.getLogger(getClass()).error("Storefront request failed: {}", request.getRequestURI(), exception);
        return page(HttpStatus.INTERNAL_SERVER_ERROR, "Có lỗi xảy ra", "Vui lòng thử lại sau.");
    }

    private ModelAndView page(HttpStatus status, String title, String message) {
        var result = new ModelAndView("store/error");
        result.setStatus(status); result.addObject("title", title); result.addObject("message", message);
        return result;
    }
}
