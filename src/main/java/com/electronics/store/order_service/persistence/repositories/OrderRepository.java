package com.electronics.store.order_service.persistence.repositories;

import com.electronics.store.order_service.persistence.enums.OrderStatus;
import com.electronics.store.order_service.persistence.model.Order;
import jakarta.persistence.LockModeType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
            SELECT o
            FROM Order o
            JOIN FETCH o.items
            WHERE o.id = :id
            """)
    Optional<Order> findOrderWithItemsById(UUID id);

    @EntityGraph(attributePaths = "items")
    List<Order> findOrdersByCustomerId(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Order o WHERE o.id = :id")
    int removeById(@NonNull @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.items
            WHERE o.id = :orderId
            """)
    Optional<Order> findOrderWithItemsByIdForUpdate(@NonNull @Param("orderId") UUID orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o SET o.status = :status
            WHERE o.id = :orderId
            """)
    void updateStatus(@Param("orderId") UUID orderId, @NonNull @Param("status") OrderStatus status);
}
