package com.electronics.store.order_service.persistence.repositories;

import com.electronics.store.order_service.persistence.model.OrderEvent;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    Optional<OrderEvent> findByOrderId(UUID orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OrderEvent oe WHERE oe.orderId = :orderId")
    int removeByOrderId(@NonNull @Param("orderId") UUID orderId);

    @Query(value = """
            SELECT * FROM order_events
            WHERE status = 'NEW'
            ORDER BY created_at LIMIT :batchSize
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OrderEvent> findFreshEvents(@Param("batchSize") int batchSize);
}
