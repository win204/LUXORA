package com.luxora.commerce.loyalty.repository;

import com.luxora.commerce.loyalty.model.LoyaltyTransaction;
import com.luxora.commerce.loyalty.model.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, UUID> {

    Page<LoyaltyTransaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    boolean existsByAccountIdAndOrderIdAndType(UUID accountId, UUID orderId, LoyaltyTransactionType type);
}
