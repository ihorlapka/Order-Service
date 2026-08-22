package com.electronics.store.order_service.persistence.repositories;

import com.electronics.store.order_service.persistence.model.Order;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findOrdersByCustomerId(UUID customerId);

    @Modifying
    @Query("DELETE FROM Order o WHERE o.id = :id")
    int removeById(@NonNull @Param("id") UUID id);
}
