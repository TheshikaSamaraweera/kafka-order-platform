package com.bigdata.order_aggregation_service.config;

import com.bigdata.order_aggregation_service.model.ProductStatistics;
import com.bigdata.schema.Order;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableKafkaStreams
@Slf4j
public class KafkaStreamsConfig {

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    public static final String ORDERS_TOPIC = "orders";
    public static final String PRODUCT_STATS_STORE = "product-statistics-store";
    public static final String WINDOWED_STATS_STORE = "windowed-statistics-store";


    private SpecificAvroSerde<Order> orderSerde() {
        SpecificAvroSerde<Order> serde = new SpecificAvroSerde<>();
        Map<String, String> config = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl
        );
        serde.configure(config, false);
        return serde;
    }


    private Serde<ProductStatistics> statsSerde() {
        return new JsonSerde<>(ProductStatistics.class);
    }


    @Bean
    public KTable<String, ProductStatistics> productStatisticsTable(StreamsBuilder builder) {


        KeyValueBytesStoreSupplier storeSupplier =
                Stores.persistentKeyValueStore(PRODUCT_STATS_STORE);


        KStream<String, Order> ordersStream = builder
                .stream(ORDERS_TOPIC, Consumed.with(Serdes.String(), orderSerde()))
                .peek((key, order) ->
                        log.info("📊 Processing order for aggregation: {} - {} - ${}",
                                order.getOrderId(), order.getProduct(), order.getPrice())
                );


        KTable<String, ProductStatistics> statsTable = ordersStream
                .groupBy(
                        (orderId, order) -> order.getProduct().toString(),
                        Grouped.with(Serdes.String(), orderSerde())
                )
                .aggregate(
                        () -> ProductStatistics.builder()
                                .orderCount(0L)
                                .totalRevenue(0.0)
                                .averagePrice(0.0)
                                .minPrice(0f)
                                .maxPrice(0f)
                                .lastUpdated(System.currentTimeMillis())
                                .build(),
                        (product, order, stats) -> {
                            stats.setProduct(product);
                            stats.update(order.getPrice());
                            return stats;
                        },
                        Materialized.<String, ProductStatistics>as(storeSupplier)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(statsSerde())
                );


        statsTable.toStream().foreach((product, stats) ->
                log.info(" Stats Update | Product: {} | Count: {} | Avg: ${:.2f} | Min: ${:.2f} | Max: ${:.2f} | Revenue: ${:.2f}",
                        product, stats.getOrderCount(), stats.getAveragePrice(),
                        stats.getMinPrice(), stats.getMaxPrice(), stats.getTotalRevenue())
        );

        return statsTable;
    }


    @Bean
    public KTable<Windowed<String>, ProductStatistics> windowedStatistics(StreamsBuilder builder) {

        KStream<String, Order> ordersStream = builder
                .stream(ORDERS_TOPIC, Consumed.with(Serdes.String(), orderSerde()));

        return ordersStream
                .groupBy(
                        (orderId, order) -> order.getProduct().toString(),
                        Grouped.with(Serdes.String(), orderSerde())
                )
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(10)))
                .aggregate(
                        () -> ProductStatistics.builder()
                                .orderCount(0L)
                                .totalRevenue(0.0)
                                .averagePrice(0.0)
                                .minPrice(0f)
                                .maxPrice(0f)
                                .build(),
                        (product, order, stats) -> {
                            stats.setProduct(product);
                            stats.update(order.getPrice());
                            return stats;
                        },
                        Materialized.<String, ProductStatistics>as(
                                        Stores.persistentWindowStore(
                                                WINDOWED_STATS_STORE,
                                                Duration.ofMinutes(5),    // retention > window size
                                                Duration.ofSeconds(10),   // window size
                                                false
                                        )
                                )
                                .withKeySerde(Serdes.String())
                                .withValueSerde(statsSerde())
                );
    }
}