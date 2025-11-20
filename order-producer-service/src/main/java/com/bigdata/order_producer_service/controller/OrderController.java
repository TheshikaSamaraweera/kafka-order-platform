package com.bigdata.order_producer_service.controller;

import com.bigdata.schema.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @PostMapping("/random")
    public String sendRandomOrder() {

        Order order = Order.newBuilder()
                .setOrderId(UUID.randomUUID().toString())
                .setProduct("Item" + new Random().nextInt(5))
                .setPrice((float) (10 + Math.random() * 90))
                .build();

        kafkaTemplate.send("orders", (String) order.getOrderId(), order);

        return "Order sent: " + order;
    }
}
