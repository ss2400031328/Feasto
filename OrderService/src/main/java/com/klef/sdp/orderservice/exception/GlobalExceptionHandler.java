package com.klef.sdp.orderservice.exception;

import java.time.LocalDateTime;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellation(
            OrderCancellationException exception) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatus(
            InvalidOrderStatusException exception) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(OrderRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderRequest(
            OrderRequestException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamFailure(
            DownstreamServiceException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedFeignFailure(
            FeignException exception) {
        return error(HttpStatus.BAD_GATEWAY,
                "A downstream service failed while processing the order");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request body is invalid");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + " - " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }
}