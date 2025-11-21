package com.bigdata.order_consumer_service.exception;

public class ErrorCategory {

    // Temporary Errors (Retryable)
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    public static final String DATABASE_TIMEOUT = "DATABASE_TIMEOUT";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String CIRCUIT_BREAKER_OPEN = "CIRCUIT_BREAKER_OPEN";

    // Permanent Errors (Not Retryable)
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String DUPLICATE_ORDER = "DUPLICATE_ORDER";
    public static final String INSUFFICIENT_INVENTORY = "INSUFFICIENT_INVENTORY";
    public static final String INVALID_PRICE = "INVALID_PRICE";
    public static final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
}