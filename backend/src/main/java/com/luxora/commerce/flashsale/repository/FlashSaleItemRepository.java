package com.luxora.commerce.flashsale.repository;

import com.luxora.commerce.flashsale.model.FlashSaleItem;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID> {

    List<FlashSaleItem> findByFlashSaleId(UUID flashSaleId);

    @Query("SELECT i FROM FlashSaleItem i WHERE i.flashSale.status = 'ACTIVE' " +
           "AND i.flashSale.startTime <= :now AND i.flashSale.endTime >= :now " +
           "AND (i.variant.id = :variantId OR (i.variant IS NULL AND i.product.id = :productId)) " +
           "AND i.soldQuantity < i.stockQuantity")
    List<FlashSaleItem> findActiveItemsForVariantOrProduct(
        @Param("now") OffsetDateTime now,
        @Param("variantId") UUID variantId,
        @Param("productId") UUID productId
    );

    default Optional<FlashSaleItem> findActiveItem(OffsetDateTime now, UUID variantId, UUID productId) {
        List<FlashSaleItem> items = findActiveItemsForVariantOrProduct(now, variantId, productId);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM FlashSaleItem i WHERE i.id = :id")
    Optional<FlashSaleItem> findByIdForUpdate(@Param("id") UUID id);
}
