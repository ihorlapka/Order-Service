package com.electronics.store.order_service.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@ToString(exclude = "order")
@Getter
@Setter
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "price", length = 20)
    private BigDecimal price;

    @Column(name = "description")
    private String description;

    @Column(name = "item_url", nullable = false)
    private String itemUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    public OrderItem(UUID itemId, int quantity, Order order) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.order = order;
    }
}
