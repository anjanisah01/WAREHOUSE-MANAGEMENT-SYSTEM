package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * One line item on a sales order (e.g. "100 units of SKU-1001").
 * Tracks both the quantity requested and the quantity actually allocated from stock.
 */
@Entity
@Getter @Setter
public class SalesOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // Parent order header
    @JoinColumn(name = "order_id")
    private SalesOrder orderRef;

    @ManyToOne(optional = false)                 // The product being ordered
    private Product product;

    private Integer requestedQty;                // Quantity the customer ordered
    private Integer allocatedQty;                // Quantity allocated from available stock
}
