package com.luxora.commerce.order.repository;

import com.luxora.commerce.order.model.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    @EntityGraph(attributePaths = "changedByUser")
    List<OrderStatusHistory> findByOrder_IdOrderByChangedAtAsc(UUID orderId);
}