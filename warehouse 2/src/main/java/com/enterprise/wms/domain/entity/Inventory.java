package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * On-hand stock for one product in one warehouse (optionally per lot/batch).
 * Unique constraint: (product + warehouse + lotBatch) — no duplicates.
 */
@Entity
@Getter @Setter
@Table(name = "inventory",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_inventory_product_warehouse_lot",
           columnNames = {"product_id", "warehouse_id", "lot_batch_id"}))
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // Which product this stock row is for
    private Product product;

    @ManyToOne(optional = false)                 // Which warehouse holds this stock
    private Warehouse warehouse;

    @ManyToOne                                   // Physical slot (nullable if not yet put-away)
    private Location location;

    @ManyToOne                                   // Lot/batch grouping (nullable for un-lotted items)
    private LotBatch lotBatch;

    private Integer quantity;                    // Available on-hand units
    private Integer reservedQty;                 // Units reserved by open orders
}
