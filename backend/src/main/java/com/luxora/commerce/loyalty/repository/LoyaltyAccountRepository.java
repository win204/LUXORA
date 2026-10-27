package com.luxora.commerce.loyalty.repository;

import com.luxora.commerce.loyalty.model.LoyaltyAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, UUID> {

    Optional<LoyaltyAccount> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccount a where a.user.id = :userId")
    Optional<LoyaltyAccount> findByUserIdForUpdate(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);

    @Query("select a.tier, count(a) from LoyaltyAccount a group by a.tier")
    java.util.List<Object[]> countByTierGrouped();
}
