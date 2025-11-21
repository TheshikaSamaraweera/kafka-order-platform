package com.bigdata.order_consumer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaRetryTopic;

@EnableKafka
@SpringBootApplication
public class OrderConsumerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderConsumerServiceApplication.class, args);
	}

}
