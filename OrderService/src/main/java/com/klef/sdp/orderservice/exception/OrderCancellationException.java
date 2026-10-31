package com.klef.sdp.orderservice.exception;

public class OrderCancellationException extends RuntimeException {

    public OrderCancellationException(String message) {
        super(message);
    }
}