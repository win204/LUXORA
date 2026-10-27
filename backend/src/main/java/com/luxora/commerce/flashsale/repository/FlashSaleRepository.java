package com.luxora.commerce.flashsale.repository;

import com.luxora.commerce.flashsale.model.FlashSale;
import com.luxora.commerce.flashsale.model.FlashSaleStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    @Query("SELECT DISTINCT f FROM FlashSale f LEFT JOIN FETCH f.items WHERE f.status = :status AND f.startTime <= :now AND f.endTime >= :now ORDER BY f.startTime ASC")
    List<FlashSale> findActiveSales(@Param("status") FlashSaleStatus status, @Param("now") OffsetDateTime now);

    default Optional<FlashSale> findFirstActiveSale(OffsetDateTime now) {
        List<FlashSale> active = findActiveSales(FlashSaleStatus.ACTIVE, now);
        return active.isEmpty() ? Optional.empty() : Optional.of(active.get(0));
    }

    List<FlashSale> findAllByOrderByStartTimeDesc();

    List<FlashSale> findByStatusOrderByStartTimeAsc(FlashSaleStatus status);
}
