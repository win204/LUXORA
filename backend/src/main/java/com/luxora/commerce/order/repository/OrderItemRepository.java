package com.luxora.commerce.order.repository;

import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("""
            select i.productId, i.productName, i.productSlug, i.imageUrl, sum(i.quantity), sum(i.lineTotal)
            from OrderItem i
            join i.order o
            where o.status in :statuses
            group by i.productId, i.productName, i.productSlug, i.imageUrl
            order by sum(i.quantity) desc
            """)
    List<Object[]> findTopSellingProducts(
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable);
}
