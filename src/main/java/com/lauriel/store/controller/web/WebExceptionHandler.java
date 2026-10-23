package com.lauriel.store.controller.web;

import com.lauriel.store.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = "com.lauriel.store.controller.web")
@org.springframework.core.annotation.Order(
    org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1
)
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView missing(ResourceNotFoundException e) {
        return page(404, e.getMessage());
    }

    @ExceptionHandler({
        BadRequestException.class,
        DuplicateResourceException.class,
        ResourceConflictException.class,
        org.springframework.web.bind.MethodArgumentNotValidException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    })
    public ModelAndView business(Exception e) {
        return page(
            e instanceof DuplicateResourceException ||
                e instanceof ResourceConflictException
                ? 409
                : 400,
            e instanceof BadRequestException ||
                e instanceof DuplicateResourceException ||
                e instanceof ResourceConflictException
                ? e.getMessage()
                : "Dữ liệu biểu mẫu không hợp lệ."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView denied() {
        return page(403, "Bạn không có quyền truy cập.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView failure(Exception e, HttpServletRequest request) {
        LoggerFactory.getLogger(getClass()).error(
            "Web request failed: {}",
            request.getRequestURI(),
            e
        );
        return page(500, "Không thể hoàn tất yêu cầu. Vui lòng thử lại.");
    }

    private ModelAndView page(int status, String message) {
        var view = new ModelAndView(
            "error/" +
                (status == 404
                    ? 404
                    : status == 403
                      ? 403
                      : status == 500
                        ? 500
                        : 400)
        );
        view.setStatus(HttpStatus.valueOf(status));
        view.addObject("message", message);
        return view;
    }
}
