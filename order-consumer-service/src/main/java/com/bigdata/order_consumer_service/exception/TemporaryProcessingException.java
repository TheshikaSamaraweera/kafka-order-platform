package com.bigdata.order_consumer_service.exception;

public class TemporaryProcessingException extends RuntimeException {
    public TemporaryProcessingException(String message) {
        super(message);
    }
}