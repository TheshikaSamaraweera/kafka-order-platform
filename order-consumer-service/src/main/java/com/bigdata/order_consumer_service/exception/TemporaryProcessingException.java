package com.bigdata.order_consumer_service.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemporaryProcessingException extends RuntimeException {
    private final String category;

    public TemporaryProcessingException(String category, String message) {
        super(message);
        this.category = category;
    }

    public TemporaryProcessingException(String category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }


}