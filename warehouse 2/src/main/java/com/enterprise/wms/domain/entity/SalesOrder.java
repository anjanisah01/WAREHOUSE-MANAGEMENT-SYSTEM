package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An outbound sales order header.
 * Progresses through: CREATED → ALLOCATED → PICKED → PACKED → SHIPPED.
 */
@Entity
@Getter @Setter
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)      // Order number, e.g. "ORD-2024-001"
    private String orderNo;

    @ManyToOne(optional = false)                  // Warehouse fulfilling this order
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)                  // Current lifecycle status
    private OrderStatus status;

    private LocalDateTime createdAt;              // When the order was placed
}
