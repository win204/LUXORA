package com.luxora.commerce.order.repository;

import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderStatus;
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

public interface OrderRepository extends JpaRepository<Order, UUID> {

    long countByStatus(OrderStatus status);

    @Query("""
            select o from Order o
            left join o.shipment shipment
            where (:status is null or o.status = :status)
              and (:orderId is null or o.id = :orderId)
              and (:customerEmail is null or lower(o.user.email) like lower(concat('%', :customerEmail, '%')))
              and (:trackingNumber is null or lower(shipment.trackingNumber) like lower(concat('%', :trackingNumber, '%')))
              and (:dateFrom is null or o.createdAt >= :dateFrom)
              and (:dateTo is null or o.createdAt <= :dateTo)
            """)
    Page<Order> findAdminPage(
            @Param("status") OrderStatus status,
            @Param("orderId") UUID orderId,
            @Param("customerEmail") String customerEmail,
            @Param("trackingNumber") String trackingNumber,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items", "shipment"})
    @Query("select distinct o from Order o where o.id in :ids")
    List<Order> findAdminSummariesByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"user", "items", "shipment"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findAdminById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "items", "shipment"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findAdminByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @Query("select o from Order o where o.user.id = :userId")
    Page<Order> findPageByUserId(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.user.id = :userId and o.id in :ids")
    List<Order> findAllWithItemsByUserIdAndIdIn(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.id = :id and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);
}