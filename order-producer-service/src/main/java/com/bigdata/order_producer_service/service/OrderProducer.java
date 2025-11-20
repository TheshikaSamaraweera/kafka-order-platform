package com.bigdata.order_producer_service.service;

import com.bigdata.order_producer_service.dto.OrderRequest;
import com.bigdata.order_producer_service.util.CorrelationId;
import com.bigdata.schema.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @Value("${app.kafka.topic}")
    private String topic;

    public void sendOrder(OrderRequest request) {

        String cid = CorrelationId.generate();

        // convert DTO → Avro Order
        Order order = Order.newBuilder()
                .setOrderId(request.orderId())
                .setProduct(request.product())
                .setPrice((float) request.price())
                .build();

        ProducerRecord<String, Order> record =
                new ProducerRecord<>(topic, request.orderId(), order);

        // Add correlation ID header
        record.headers().add("cid", cid.getBytes());

        log.info("Producing order | cid={} | id={} | product={} | price={}",
                cid, order.getOrderId(), order.getProduct(), order.getPrice());

        kafkaTemplate.send(record);
    }
}