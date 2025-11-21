package com.bigdata.order_aggregation_service.controller;

import com.bigdata.order_aggregation_service.config.KafkaStreamsConfig;
import com.bigdata.order_aggregation_service.model.ProductStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;


    @GetMapping("/product/{productName}")
    public ResponseEntity<ProductStatistics> getProductStatistics(
            @PathVariable String productName
    ) {
        try {
            ReadOnlyKeyValueStore<String, ProductStatistics> store = getStore();
            ProductStatistics stats = store.get(productName);

            if (stats == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error querying statistics for product: {}", productName, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/all")
    public ResponseEntity<Map<String, ProductStatistics>> getAllStatistics() {
        try {
            ReadOnlyKeyValueStore<String, ProductStatistics> store = getStore();
            Map<String, ProductStatistics> allStats = new HashMap<>();

            try (KeyValueIterator<String, ProductStatistics> iterator = store.all()) {
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    allStats.put(entry.key, entry.value);
                }
            }

            return ResponseEntity.ok(allStats);

        } catch (Exception e) {
            log.error("Error querying all statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        try {
            ReadOnlyKeyValueStore<String, ProductStatistics> store = getStore();

            long totalOrders = 0;
            double totalRevenue = 0.0;
            int productCount = 0;

            try (KeyValueIterator<String, ProductStatistics> iterator = store.all()) {
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    ProductStatistics stats = entry.value;
                    totalOrders += stats.getOrderCount();
                    totalRevenue += stats.getTotalRevenue();
                    productCount++;
                }
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalOrders", totalOrders);
            summary.put("totalRevenue", totalRevenue);
            summary.put("productCount", productCount);
            summary.put("averageRevenuePerProduct",
                    productCount > 0 ? totalRevenue / productCount : 0.0);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error generating summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        Map<String, String> health = new HashMap<>();
        health.put("status", streams != null && streams.state().isRunningOrRebalancing()
                ? "UP" : "DOWN");
        health.put("state", streams != null ? streams.state().toString() : "UNKNOWN");

        return ResponseEntity.ok(health);
    }


    private ReadOnlyKeyValueStore<String, ProductStatistics> getStore() {
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();

        if (streams == null) {
            throw new IllegalStateException("KafkaStreams is not initialized");
        }

        return streams.store(
                StoreQueryParameters.fromNameAndType(
                        KafkaStreamsConfig.PRODUCT_STATS_STORE,
                        QueryableStoreTypes.keyValueStore()
                )
        );
    }
}