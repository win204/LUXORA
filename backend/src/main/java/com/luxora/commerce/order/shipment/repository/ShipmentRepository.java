package com.luxora.commerce.order.shipment.repository;

import com.luxora.commerce.order.shipment.model.Shipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    boolean existsByOrder_Id(UUID orderId);

    Optional<Shipment> findByOrder_Id(UUID orderId);
}
