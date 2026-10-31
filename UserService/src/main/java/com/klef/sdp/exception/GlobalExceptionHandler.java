package com.klef.sdp.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleNotFound(ResourceNotFoundException ex)
    {
        return response(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>>
    handleUnauthorized(UnauthorizedException ex)
    {
        return response(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDenied(AccessDeniedException ex)
    {
        return response(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgument(IllegalArgumentException ex)
    {
        return response(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidation(MethodArgumentNotValidException ex)
    {
        Map<String, String> errors =
                new HashMap<>();

        ex.getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errors.put(
                    error.getField(),
                    error.getDefaultMessage()
                )
            );

        Map<String, Object> body =
                new HashMap<>();

        body.put(
            "timestamp",
            LocalDateTime.now()
        );

        body.put(
            "status",
            HttpStatus.BAD_REQUEST.value()
        );

        body.put(
            "error",
            "Validation Failed"
        );

        body.put(
            "errors",
            errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    private ResponseEntity<Map<String, Object>>
    response(
        HttpStatus status,
        String message
    )
    {
        Map<String, Object> body =
                new HashMap<>();

        body.put(
            "timestamp",
            LocalDateTime.now()
        );

        body.put(
            "status",
            status.value()
        );

        body.put(
            "error",
            status.getReasonPhrase()
        );

        body.put(
            "message",
            message
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}