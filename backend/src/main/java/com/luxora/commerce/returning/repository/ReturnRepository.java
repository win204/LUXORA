package com.luxora.commerce.returning.repository;

import com.luxora.commerce.returning.model.ReturnRequest;
import com.luxora.commerce.returning.model.ReturnStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRepository extends JpaRepository<ReturnRequest, UUID> {

    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select r from ReturnRequest r where r.user.id = :userId")
    Page<ReturnRequest> findPageByUserId(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select r from ReturnRequest r where r.user.id = :userId and r.id = :id")
    Optional<ReturnRequest> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select r from ReturnRequest r where r.user.id = :userId and r.id = :id")
    Optional<ReturnRequest> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select r from ReturnRequest r
            left join r.shipment shipment
            where (:status is null or r.status = :status)
              and (:orderId is null or r.order.id = :orderId)
              and (:customerEmail is null or lower(r.user.email) like lower(concat('%', :customerEmail, '%')))
              and (:trackingNumber is null or lower(shipment.trackingNumber) like lower(concat('%', :trackingNumber, '%')))
              and (:dateFrom is null or r.requestedAt >= :dateFrom)
              and (:dateTo is null or r.requestedAt <= :dateTo)
            """)
    Page<ReturnRequest> findAdminPage(
            @Param("status") ReturnStatus status,
            @Param("orderId") UUID orderId,
            @Param("customerEmail") String customerEmail,
            @Param("trackingNumber") String trackingNumber,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);

    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select distinct r from ReturnRequest r where r.id in :ids")
    List<ReturnRequest> findAdminSummariesByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select r from ReturnRequest r where r.id = :id")
    Optional<ReturnRequest> findAdminById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"order", "user", "items", "items.orderItem", "shipment"})
    @Query("select r from ReturnRequest r where r.id = :id")
    Optional<ReturnRequest> findAdminByIdForUpdate(@Param("id") UUID id);

    @Query("select ri.orderItem.id, coalesce(sum(ri.requestedQuantity), 0) from ReturnItem ri where ri.orderItem.id in :orderItemIds and ri.returnRequest.status in :statuses group by ri.orderItem.id")
    List<Object[]> activeQuantitiesByOrderItemIds(@Param("orderItemIds") Collection<UUID> orderItemIds, @Param("statuses") Collection<ReturnStatus> statuses);
}