package com.electronics.store.order_service.exception.handling;

import com.electronics.store.order_service.persistence.service.exceptions.NotEnoughItemsException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderCancellationIsNotAllowedException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderEventNotFoundException;
import com.electronics.store.order_service.persistence.service.exceptions.OrderIsAlreadyCancelledException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotEnoughItemsException.class)
    public ResponseEntity<ApiError> handleNotEnoughItems(NotEnoughItemsException ex, HttpServletRequest request) {
        log.warn("Order rejected, not enough items: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(OrderEventNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderEventNotFound(OrderEventNotFoundException ex, HttpServletRequest request) {
        log.warn("Order event not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(OrderIsAlreadyCancelledException.class)
    public ResponseEntity<ApiError> handleAlreadyCancelled(OrderIsAlreadyCancelledException ex, HttpServletRequest request) {
        log.info("Cancel rejected, order already cancelled: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(OrderCancellationIsNotAllowedException.class)
    public ResponseEntity<ApiError> handleCancellationNotAllowed(OrderCancellationIsNotAllowedException ex,
                                                                 HttpServletRequest request) {
        log.info("Cancel rejected, not allowed at current stage: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("Request body validation failed for {}: {}", request.getRequestURI(), fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new ApiError.FieldError(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();
        log.warn("Constraint violation for {}: {}", request.getRequestURI(), fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body for {}", request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while processing {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status,
                                                   String message,
                                                   HttpServletRequest request,
                                                   List<ApiError.FieldError> fieldErrors) {
        ApiError error = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(error);
    }

    public record ApiError(
            OffsetDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            List<FieldError> fieldErrors) {

        public record FieldError(String field, String message) { }
    }
}
