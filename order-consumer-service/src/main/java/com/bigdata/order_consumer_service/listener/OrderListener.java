package com.bigdata.order_consumer_service.listener;

import com.bigdata.order_consumer_service.entity.FailedOrderEntity;
import com.bigdata.order_consumer_service.exception.ErrorCategory;
import com.bigdata.order_consumer_service.exception.PermanentProcessingException;
import com.bigdata.order_consumer_service.exception.TemporaryProcessingException;
import com.bigdata.order_consumer_service.repository.FailedOrderRepository;
import com.bigdata.schema.Order;
import lombok.RequiredArgsConstructor;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderListener {

    private final FailedOrderRepository failedOrderRepository;
    private final Random random = new Random();

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 2000,      // 2 seconds
                    multiplier = 3.0,  // 2s, 6s, 18s
                    maxDelay = 30000   // cap at 30 seconds
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


        int retryAttempt = 0;
        if (topic != null && topic.contains("-retry-")) {
            try {
                retryAttempt = Integer.parseInt(topic.substring(topic.lastIndexOf("-") + 1)) + 1;
            } catch (Exception e) {
                retryAttempt = 0;
            }
        }

        log.info(" [Attempt {}] Consuming from: {} | cid={} | Order: {} | Product: {} | Price: ${}",
                retryAttempt, topic, cid, order.getOrderId(), order.getProduct(), order.getPrice());

        try {
            processOrder(order, cid);

            // Manual acknowledgment (commit offset)
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info(" Successfully processed order: {} | Product: {}",
                    order.getOrderId(), order.getProduct());

        } catch (TemporaryProcessingException e) {
            log.warn(" [Attempt {}] Temporary failure | Order: {} | Category: {} | Reason: {} - WILL RETRY",
                    retryAttempt, order.getOrderId(), e.getCategory(), e.getMessage());
            throw e;

        } catch (PermanentProcessingException e) {
            log.error(" Permanent failure | Order: {} | Category: {} | Reason: {} - SENDING TO DLQ",
                    order.getOrderId(), e.getCategory(), e.getMessage());
            throw e;
        }
    }


    @DltHandler
    public void handleDlt(
            Order order,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            @Header(KafkaHeaders.EXCEPTION_STACKTRACE) String stackTrace,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String receivedTopic,
            @Header(value = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(value = "cid", required = false) byte[] cidBytes
    ) {
        String cid = cidBytes != null ? new String(cidBytes) : "N/A";


        int retryCount = 0;
        if (receivedTopic != null && receivedTopic.contains("-retry-")) {
            try {
                retryCount = Integer.parseInt(receivedTopic.substring(receivedTopic.lastIndexOf("-") + 1)) + 1;
            } catch (Exception e) {
                retryCount = 0;
            }
        }

        log.error(" DLQ HANDLER | cid={} | Order: {} | Product: {} | Retries: {} | Reason: {}",
                cid, order.getOrderId(), order.getProduct(), retryCount, exceptionMessage);


        String failureType = exceptionMessage.contains("Temporary") ? "TEMPORARY" : "PERMANENT";
        String failureCategory = extractCategory(exceptionMessage);


        FailedOrderEntity failedOrder = FailedOrderEntity.builder()
                .orderId(order.getOrderId().toString())
                .product(order.getProduct().toString())
                .price(order.getPrice())
                .failureType(failureType)
                .failureCategory(failureCategory)
                .errorMessage(exceptionMessage)
                .stackTrace(stackTrace)
                .retryCount(retryCount)
                .originalTopic(originalTopic != null ? originalTopic : "orders")
                .correlationId(cid)
                .failedAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        failedOrderRepository.save(failedOrder);

        log.info(" Saved failed order to database | ID: {} | Order: {}",
                failedOrder.getId(), order.getOrderId());
    }


    private void processOrder(Order order, String cid) {
        String orderId = order.getOrderId().toString();
        String product = order.getProduct().toString();
        float price = order.getPrice();


        validateOrder(orderId, product, price);


        checkBusinessRules(orderId, product, price);


        callExternalServices(orderId);


        log.info(" Processing order | cid={} | Order: {} | Product: {} | Price: ${}",
                cid, orderId, product, price);


        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info(" Order processed successfully | Order: {}", orderId);
    }

    /**
     * Validation - Permanent Failures
     */
    private void validateOrder(String orderId, String product, float price) {

        // Simulate: Invalid price
        if (price <= 0) {
            throw new PermanentProcessingException(
                    ErrorCategory.INVALID_PRICE,
                    "Price must be greater than zero: " + price
            );
        }

        // Simulate: Price too high (business rule)
        if (price > 10000) {
            throw new PermanentProcessingException(
                    ErrorCategory.VALIDATION_ERROR,
                    "Price exceeds maximum allowed: $" + price
            );
        }

        // Simulate: Empty product name
        if (product == null || product.trim().isEmpty()) {
            throw new PermanentProcessingException(
                    ErrorCategory.VALIDATION_ERROR,
                    "Product name cannot be empty"
            );
        }

        // Demo: Trigger validation error with specific order ID pattern
        if (orderId.endsWith("88")) {
            throw new PermanentProcessingException(
                    ErrorCategory.VALIDATION_ERROR,
                    "Demo: Invalid order format for order: " + orderId
            );
        }
    }


    private void checkBusinessRules(String orderId, String product, float price) {

        // Simulate: Duplicate order detection
        if (orderId.endsWith("77")) {
            throw new PermanentProcessingException(
                    ErrorCategory.DUPLICATE_ORDER,
                    "Order already exists: " + orderId
            );
        }

        // Simulate: Product not in catalog
        if (orderId.endsWith("66")) {
            throw new PermanentProcessingException(
                    ErrorCategory.PRODUCT_NOT_FOUND,
                    "Product not found in catalog: " + product
            );
        }

        // Simulate: Insufficient inventory
        if (orderId.endsWith("55")) {
            throw new PermanentProcessingException(
                    ErrorCategory.INSUFFICIENT_INVENTORY,
                    "Insufficient inventory for product: " + product
            );
        }
    }


    private void callExternalServices(String orderId) {


        if (orderId.endsWith("99")) {
            throw new TemporaryProcessingException(
                    ErrorCategory.NETWORK_ERROR,
                    "Network timeout while calling payment service"
            );
        }


        if (orderId.endsWith("98")) {
            throw new TemporaryProcessingException(
                    ErrorCategory.DATABASE_TIMEOUT,
                    "Database connection timeout - retry will likely succeed"
            );
        }


        if (orderId.endsWith("97")) {
            throw new TemporaryProcessingException(
                    ErrorCategory.SERVICE_UNAVAILABLE,
                    "Inventory service returned 503 - Service Unavailable"
            );
        }


        if (orderId.endsWith("96")) {
            throw new TemporaryProcessingException(
                    ErrorCategory.RATE_LIMIT_EXCEEDED,
                    "Rate limit exceeded - backing off"
            );
        }


        if (orderId.endsWith("95")) {
            if (random.nextInt(10) < 3) {
                throw new TemporaryProcessingException(
                        ErrorCategory.SERVICE_UNAVAILABLE,
                        "Random transient failure - will likely succeed on retry"
                );
            }
        }
    }


    private String extractCategory(String message) {
        if (message.contains(ErrorCategory.NETWORK_ERROR)) return ErrorCategory.NETWORK_ERROR;
        if (message.contains(ErrorCategory.DATABASE_TIMEOUT)) return ErrorCategory.DATABASE_TIMEOUT;
        if (message.contains(ErrorCategory.SERVICE_UNAVAILABLE)) return ErrorCategory.SERVICE_UNAVAILABLE;
        if (message.contains(ErrorCategory.VALIDATION_ERROR)) return ErrorCategory.VALIDATION_ERROR;
        if (message.contains(ErrorCategory.DUPLICATE_ORDER)) return ErrorCategory.DUPLICATE_ORDER;
        if (message.contains(ErrorCategory.INVALID_PRICE)) return ErrorCategory.INVALID_PRICE;
        if (message.contains(ErrorCategory.PRODUCT_NOT_FOUND)) return ErrorCategory.PRODUCT_NOT_FOUND;
        if (message.contains(ErrorCategory.INSUFFICIENT_INVENTORY)) return ErrorCategory.INSUFFICIENT_INVENTORY;
        return "UNKNOWN";
    }
}