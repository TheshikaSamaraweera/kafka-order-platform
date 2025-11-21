package com.bigdata.order_consumer_service.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "failed_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private Float price;

    @Column(nullable = false, length = 50)
    private String failureType; // TEMPORARY, PERMANENT

    @Column(nullable = false, length = 100)
    private String failureCategory; // NETWORK_ERROR, VALIDATION_ERROR,

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private String originalTopic;

    private String correlationId;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, REPROCESSED, DISCARDED

    private LocalDateTime reprocessedAt;

    private String reprocessedBy;
}