package com.luxora.commerce.catalog.repository;

import com.luxora.commerce.catalog.model.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    @EntityGraph(attributePaths = {"product", "inventoryItem"})
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findAdminById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"product", "product.brand", "product.category", "product.images", "inventoryItem"})
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findWithProductById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "product.brand", "product.category", "product.images", "inventoryItem"})
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findWithProductByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "inventoryItem")
    @Query("select v from ProductVariant v where v.id in :ids order by v.id")
    List<ProductVariant> findAllByIdInForUpdate(@Param("ids") Collection<UUID> ids);
}