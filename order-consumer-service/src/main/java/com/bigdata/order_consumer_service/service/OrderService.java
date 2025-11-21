package com.bigdata.order_consumer_service.service;

import com.bigdata.order_consumer_service.entity.OrderEntity;
import com.bigdata.order_consumer_service.exception.ErrorCategory;
import com.bigdata.order_consumer_service.exception.PermanentProcessingException;
import com.bigdata.order_consumer_service.repository.OrderRepository;
import com.bigdata.schema.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Transactional
    public OrderEntity saveOrder(Order order, String correlationId,
                                 String topic, Integer partition, Long offset) {

        String orderId = order.getOrderId().toString();


        if (orderRepository.existsByOrderId(orderId)) {
            log.warn("Duplicate order detected: {}", orderId);
            throw new PermanentProcessingException(
                    ErrorCategory.DUPLICATE_ORDER,
                    "Order already exists in database: " + orderId
            );
        }


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "kafka");
        metadata.put("originalMessage", String.format(
                "Order{id=%s, product=%s, price=%.2f}",
                orderId, order.getProduct(), order.getPrice()
        ));

        String metadataJson = null;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata", e);
        }


        OrderEntity entity = OrderEntity.builder()
                .orderId(orderId)
                .product(order.getProduct().toString())
                .price(order.getPrice())
                .correlationId(correlationId)
                .receivedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .status("PROCESSED")
                .kafkaTopic(topic)
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .metadata(metadataJson)
                .build();

        OrderEntity saved = orderRepository.save(entity);

        log.info("Order saved to database | ID: {} | OrderID: {} | Product: {} | Price: ${}",
                saved.getId(), orderId, order.getProduct(), order.getPrice());

        return saved;
    }


    public OrderEntity getByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElse(null);
    }


    public boolean orderExists(String orderId) {
        return orderRepository.existsByOrderId(orderId);
    }
}