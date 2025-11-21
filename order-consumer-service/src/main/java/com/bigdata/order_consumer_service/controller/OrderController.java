package com.bigdata.order_consumer_service.controller;

import com.bigdata.order_consumer_service.entity.OrderEntity;
import com.bigdata.order_consumer_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;


    @GetMapping
    public ResponseEntity<Page<OrderEntity>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "processedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
    ) {
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<OrderEntity> orders = orderRepository.findAll(pageRequest);

        return ResponseEntity.ok(orders);
    }


    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderEntity> getOrderByOrderId(@PathVariable String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/product/{product}")
    public ResponseEntity<List<OrderEntity>> getOrdersByProduct(@PathVariable String product) {
        List<OrderEntity> orders = orderRepository.findByProduct(product);
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderEntity>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "processedAt"));
        Page<OrderEntity> orders = orderRepository.findByStatus(status, pageRequest);
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/date-range")
    public ResponseEntity<List<OrderEntity>> getOrdersInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        List<OrderEntity> orders = orderRepository.findOrdersInDateRange(start, end);
        return ResponseEntity.ok(orders);
    }


    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", orderRepository.count());
        stats.put("processedOrders", orderRepository.countByStatus("PROCESSED"));
        stats.put("totalRevenue", orderRepository.getTotalRevenue());

        // Orders in last hour
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        stats.put("ordersLastHour", orderRepository.countOrdersSince(oneHourAgo));

        // Orders in last 24 hours
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        stats.put("ordersLast24Hours", orderRepository.countOrdersSince(oneDayAgo));

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/products")
    public ResponseEntity<List<Map<String, Object>>> getProductStatistics() {
        List<Object[]> results = orderRepository.getProductStatistics();

        List<Map<String, Object>> stats = results.stream()
                .map(row -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("product", row[0]);
                    stat.put("count", row[1]);
                    stat.put("totalRevenue", row[2]);
                    stat.put("averagePrice", row[3]);
                    return stat;
                })
                .toList();

        return ResponseEntity.ok(stats);
    }


    @GetMapping("/search")
    public ResponseEntity<List<OrderEntity>> searchOrders(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String status
    ) {
        if (orderId != null && !orderId.isEmpty()) {
            return ResponseEntity.ok(
                    orderRepository.findByOrderId(orderId).stream().toList()
            );
        }

        if (product != null && !product.isEmpty()) {
            return ResponseEntity.ok(orderRepository.findByProduct(product));
        }

        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(orderRepository.findByStatus(status));
        }

        return ResponseEntity.ok(List.of());
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("totalOrders", orderRepository.count());
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }
}