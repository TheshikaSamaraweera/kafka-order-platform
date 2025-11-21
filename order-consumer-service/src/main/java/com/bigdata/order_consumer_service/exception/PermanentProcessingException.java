package com.bigdata.order_consumer_service.exception;

public class PermanentProcessingException extends RuntimeException {
    public PermanentProcessingException(String message) {
        super(message);
    }
}