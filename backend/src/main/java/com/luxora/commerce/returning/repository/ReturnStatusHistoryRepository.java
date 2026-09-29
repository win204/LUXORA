package com.luxora.commerce.returning.repository;

import com.luxora.commerce.returning.model.ReturnStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnStatusHistoryRepository extends JpaRepository<ReturnStatusHistory, UUID> {

    @EntityGraph(attributePaths = "changedByUser")
    List<ReturnStatusHistory> findByReturnRequest_IdOrderByChangedAtAsc(UUID returnId);
}
