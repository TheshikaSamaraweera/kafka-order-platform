package com.bigdata.order_producer_service.util;

import java.util.UUID;

public class CorrelationId {
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
