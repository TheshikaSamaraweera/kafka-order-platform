package com.bigdata.order_consumer_service.listener;

import com.bigdata.order_consumer_service.exception.PermanentProcessingException;
import com.bigdata.order_consumer_service.exception.TemporaryProcessingException;
import com.bigdata.schema.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderListener {

    @RetryableTopic(
            attempts = "4", // 1 original + 3 retries
            backoff = @Backoff(
                    delay = 1000,  // 1 second
                    multiplier = 5.0,  //  1s, 5s, 25s
                    maxDelay = 30000  // cap at 30 seconds
            ),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
            include = {TemporaryProcessingException.class}
    )
    @KafkaListener(topics = "orders", groupId = "order-consumer-group")
    public void listen(
            Order order,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = "cid", required = false) byte[] cidBytes,
            Acknowledgment acknowledgment
    ) {
        String cid = cidBytes != null ? new String(cidBytes) : "N/A";

        log.info(" Consuming from topic: {} | cid={} | Order ID: {} | Product: {} | Price: {}",
                topic, cid, order.getOrderId(), order.getProduct(), order.getPrice());

        try {
            processOrder(order);

            // Manual acknowledgment (commit offset)
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info(" Successfully processed order: {}", order.getOrderId());

        } catch (TemporaryProcessingException e) {
            log.warn(" Temporary failure for order {}: {} - Will retry",
                    order.getOrderId(), e.getMessage());
            throw e;

        } catch (PermanentProcessingException e) {
            log.error(" Permanent failure for order {}: {} - Sending to DLQ",
                    order.getOrderId(), e.getMessage());
            throw e;
        }
    }


    @DltHandler
    public void handleDlt(
            Order order,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(value = "cid", required = false) byte[] cidBytes
    ) {
        String cid = cidBytes != null ? new String(cidBytes) : "N/A";

        log.error("DLQ: Failed order after retries | cid={} | Order: {} | Product: {} | Reason: {}",
                cid, order.getOrderId(), order.getProduct(), exceptionMessage);


    }


    private void processOrder(Order order) {
        String orderId = order.getOrderId().toString();

        // Simulate temporary failure (retry scenario)
        if (orderId.endsWith("99")) {
            throw new TemporaryProcessingException(
                    "Simulated temporary failure for order: " + orderId
            );
        }

        // Simulate permanent failure (DLQ scenario)
        if (orderId.endsWith("00")) {
            throw new PermanentProcessingException(
                    "Simulated permanent failure for order: " + orderId
            );
        }

        // Normal processing
        log.info("Processing order business logic for: {}", orderId);


    }
}