package com.bigdata.order_consumer_service.service;

import com.bigdata.order_consumer_service.entity.FailedOrderEntity;
import com.bigdata.order_consumer_service.repository.FailedOrderRepository;
import com.bigdata.schema.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReprocessingService {

    private final FailedOrderRepository failedOrderRepository;
    private final KafkaTemplate<String, Order> kafkaTemplate;


    @Transactional
    public boolean reprocessFailedOrder(Long id, String reprocessedBy) {

        return failedOrderRepository.findById(id)
                .map(failedOrder -> {
                    if (!"PENDING".equals(failedOrder.getStatus())) {
                        log.warn("Order {} is not in PENDING status, current: {}",
                                id, failedOrder.getStatus());
                        return false;
                    }

                    try {

                        Order order = Order.newBuilder()
                                .setOrderId(failedOrder.getOrderId())
                                .setProduct(failedOrder.getProduct())
                                .setPrice(failedOrder.getPrice())
                                .build();


                        String topic = failedOrder.getOriginalTopic();
                        kafkaTemplate.send(topic, failedOrder.getOrderId(), order);


                        failedOrder.setStatus("REPROCESSED");
                        failedOrder.setReprocessedAt(LocalDateTime.now());
                        failedOrder.setReprocessedBy(reprocessedBy);
                        failedOrderRepository.save(failedOrder);

                        log.info(" Reprocessed order {} back to topic: {}",
                                failedOrder.getOrderId(), topic);

                        return true;

                    } catch (Exception e) {
                        log.error("Failed to reprocess order {}", id, e);
                        return false;
                    }
                })
                .orElse(false);
    }
}