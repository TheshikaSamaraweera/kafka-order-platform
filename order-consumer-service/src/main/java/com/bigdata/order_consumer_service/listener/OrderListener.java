package com.bigdata.order_consumer_service.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderListener {

    @KafkaListener(topics = "orders", groupId = "order-consumer-group")
    public void listen(GenericRecord record) {
        log.info("Consumed Order:");
        log.info("  Order ID: {}", record.get("orderId"));
        log.info("  Product: {}", record.get("product"));
        log.info("  Price: {}", record.get("price"));

    }
}