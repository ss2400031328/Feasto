package com.klef.sdp.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RestaurantNotFoundException.class,
            MenuItemNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateRestaurantException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateRestaurantException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + " - "
                        + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status,
            String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message,
                        LocalDateTime.now()));
    }
}
