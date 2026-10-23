package com.lauriel.store.exception;

import com.lauriel.store.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@org.springframework.core.annotation.Order(
    org.springframework.core.Ordered.LOWEST_PRECEDENCE
)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
        GlobalExceptionHandler.class
    );

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(
        ResourceNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(
        BadRequestException exception
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleValidation(
        ConstraintViolationException exception
    ) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleConstraint(
        DataIntegrityViolationException exception
    ) {
        return error(
            HttpStatus.CONFLICT,
            "Dữ liệu trùng lặp hoặc vi phạm ràng buộc"
        );
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Object> handleResourceConflict(
        ResourceConflictException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Object> handleDuplicate(
        DuplicateResourceException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(
        AuthenticationException exception
    ) {
        return error(
            HttpStatus.UNAUTHORIZED,
            "Tên đăng nhập hoặc mật khẩu không chính xác."
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(
        AccessDeniedException exception
    ) {
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập");
    }

    // Keep framework errors (validation, malformed JSON, wrong method, etc.) in the same format.
    // Never include exception messages that may contain request values or implementation details.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        Object body,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        String message = status.is5xxServerError()
            ? "Có lỗi hệ thống, vui lòng thử lại sau"
            : "Yêu cầu không hợp lệ";
        return new ResponseEntity<>(response(status, message), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Có lỗi hệ thống, vui lòng thử lại sau"
        );
    }

    private ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(response(status, message));
    }

    private ErrorResponse response(HttpStatusCode status, String message) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String reason =
            knownStatus == null ? "HTTP Error" : knownStatus.getReasonPhrase();
        return new ErrorResponse(
            LocalDateTime.now(),
            status.value(),
            reason,
            message
        );
    }
}
