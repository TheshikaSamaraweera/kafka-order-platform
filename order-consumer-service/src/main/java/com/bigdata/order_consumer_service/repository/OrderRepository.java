package com.bigdata.order_consumer_service.repository;

import com.bigdata.order_consumer_service.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    List<OrderEntity> findByProduct(String product);

    List<OrderEntity> findByStatus(String status);

    Page<OrderEntity> findByStatus(String status, Pageable pageable);

    List<OrderEntity> findByProcessedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT o.product, COUNT(o) as count, SUM(o.price) as total, AVG(o.price) as avg " +
            "FROM OrderEntity o WHERE o.status = 'PROCESSED' GROUP BY o.product")
    List<Object[]> getProductStatistics();

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.processedAt >= :startTime")
    long countOrdersSince(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT SUM(o.price) FROM OrderEntity o WHERE o.status = 'PROCESSED'")
    Double getTotalRevenue();

    @Query("SELECT o FROM OrderEntity o WHERE o.processedAt BETWEEN :start AND :end ORDER BY o.processedAt DESC")
    List<OrderEntity> findOrdersInDateRange(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}