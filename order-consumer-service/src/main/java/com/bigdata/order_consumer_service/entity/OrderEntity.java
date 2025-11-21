package com.bigdata.order_consumer_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_id", columnList = "orderId"),
        @Index(name = "idx_product", columnList = "product"),
        @Index(name = "idx_processed_at", columnList = "processedAt"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String orderId;

    @Column(nullable = false, length = 200)
    private String product;

    @Column(nullable = false)
    private Float price;

    @Column(length = 50)
    private String correlationId;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false, length = 20)
    private String status; // PROCESSED, PROCESSING, FAILED

    @Column(length = 100)
    private String kafkaTopic;

    @Column
    private Integer kafkaPartition;

    @Column
    private Long kafkaOffset;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}