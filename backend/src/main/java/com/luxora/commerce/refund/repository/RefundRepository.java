package com.luxora.commerce.refund.repository;

import com.luxora.commerce.refund.model.Refund;
import com.luxora.commerce.refund.model.RefundStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    boolean existsByOrder_IdAndStatus(UUID orderId, RefundStatus status);

    boolean existsByReturnRequest_IdAndStatus(UUID returnId, RefundStatus status);

    Optional<Refund> findTopByOrder_IdOrderByCreatedAtDesc(UUID orderId);

    Optional<Refund> findTopByOrder_IdAndStatusOrderByCreatedAtDesc(UUID orderId, RefundStatus status);

    Optional<Refund> findTopByReturnRequest_IdOrderByCreatedAtDesc(UUID returnId);

    Optional<Refund> findTopByReturnRequest_IdAndStatusOrderByCreatedAtDesc(UUID returnId, RefundStatus status);
}

