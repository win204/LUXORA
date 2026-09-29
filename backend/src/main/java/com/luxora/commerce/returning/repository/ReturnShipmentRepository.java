package com.luxora.commerce.returning.repository;

import com.luxora.commerce.returning.model.ReturnShipment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnShipmentRepository extends JpaRepository<ReturnShipment, UUID> {

    Optional<ReturnShipment> findByReturnRequest_Id(UUID returnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ReturnShipment s where s.returnRequest.id = :returnId")
    Optional<ReturnShipment> findByReturnRequest_IdForUpdate(@Param("returnId") UUID returnId);
}