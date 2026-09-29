package com.luxora.commerce.payment.repository;

import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.model.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByOrder_IdAndStatus(UUID orderId, PaymentStatus status);

    Optional<Payment> findTopByOrder_IdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findTopByOrder_IdAndStatusOrderByCreatedAtDesc(UUID orderId, PaymentStatus status);
}
