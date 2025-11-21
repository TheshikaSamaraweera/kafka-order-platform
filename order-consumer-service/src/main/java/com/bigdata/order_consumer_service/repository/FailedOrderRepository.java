package com.bigdata.order_consumer_service.repository;

import com.bigdata.order_consumer_service.entity.FailedOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FailedOrderRepository extends JpaRepository<FailedOrderEntity, Long> {

    List<FailedOrderEntity> findByStatus(String status);

    List<FailedOrderEntity> findByFailureType(String failureType);

    Optional<FailedOrderEntity> findByOrderIdAndStatus(String orderId, String status);

    long countByStatus(String status);

    long countByFailureType(String failureType);
}