package com.bigdata.order_consumer_service.controller;

import com.bigdata.order_consumer_service.entity.FailedOrderEntity;
import com.bigdata.order_consumer_service.repository.FailedOrderRepository;
import com.bigdata.order_consumer_service.service.DlqReprocessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@Slf4j
public class DlqController {

    private final FailedOrderRepository failedOrderRepository;
    private final DlqReprocessingService reprocessingService;

    /**
     * Get all failed orders
     */
    @GetMapping("/failed-orders")
    public ResponseEntity<List<FailedOrderEntity>> getAllFailedOrders(
            @RequestParam(required = false) String status
    ) {
        List<FailedOrderEntity> orders = status != null
                ? failedOrderRepository.findByStatus(status)
                : failedOrderRepository.findAll();

        return ResponseEntity.ok(orders);
    }

    /**
     * Get failed orders by failure type
     */
    @GetMapping("/failed-orders/type/{failureType}")
    public ResponseEntity<List<FailedOrderEntity>> getByFailureType(
            @PathVariable String failureType
    ) {
        List<FailedOrderEntity> orders = failedOrderRepository.findByFailureType(failureType);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get statistics about failed orders
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", failedOrderRepository.count());
        stats.put("pending", failedOrderRepository.countByStatus("PENDING"));
        stats.put("reprocessed", failedOrderRepository.countByStatus("REPROCESSED"));
        stats.put("discarded", failedOrderRepository.countByStatus("DISCARDED"));
        stats.put("temporary", failedOrderRepository.countByFailureType("TEMPORARY"));
        stats.put("permanent", failedOrderRepository.countByFailureType("PERMANENT"));

        return ResponseEntity.ok(stats);
    }

    /**
     * Reprocess a single failed order by ID
     */
    @PostMapping("/reprocess/{id}")
    public ResponseEntity<Map<String, String>> reprocessOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "system") String reprocessedBy
    ) {
        try {
            boolean success = reprocessingService.reprocessFailedOrder(id, reprocessedBy);

            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("status", "success");
                response.put("message", "Order reprocessed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "failed");
                response.put("message", "Order not found or already reprocessed");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error reprocessing order {}", id, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Reprocess all pending failed orders
     */
    @PostMapping("/reprocess-all")
    public ResponseEntity<Map<String, Object>> reprocessAllPending(
            @RequestParam(required = false, defaultValue = "system") String reprocessedBy
    ) {
        List<FailedOrderEntity> pendingOrders =
                failedOrderRepository.findByStatus("PENDING");

        int successCount = 0;
        int failedCount = 0;

        for (FailedOrderEntity order : pendingOrders) {
            try {
                boolean success = reprocessingService.reprocessFailedOrder(
                        order.getId(), reprocessedBy
                );
                if (success) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error reprocessing order {}", order.getId(), e);
                failedCount++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total", pendingOrders.size());
        response.put("success", successCount);
        response.put("failed", failedCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Discard a failed order (mark as not reprocessable)
     */
    @PostMapping("/discard/{id}")
    public ResponseEntity<Map<String, String>> discardOrder(@PathVariable Long id) {
        return failedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus("DISCARDED");
                    failedOrderRepository.save(order);

                    Map<String, String> response = new HashMap<>();
                    response.put("status", "success");
                    response.put("message", "Order discarded");
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("status", "error");
                    response.put("message", "Order not found");
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get details of a specific failed order
     */
    @GetMapping("/failed-orders/{id}")
    public ResponseEntity<FailedOrderEntity> getFailedOrderById(@PathVariable Long id) {
        return failedOrderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}