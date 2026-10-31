package com.klef.sdp.orderservice.exception;

public class OrderRequestException extends RuntimeException {

    public OrderRequestException(String message) {
        super(message);
    }

    public OrderRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
