package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A physical warehouse location (e.g. "WH-001 – Main Depot").
 * Every inventory record, order, and location belongs to exactly one warehouse.
 */
@Entity                                          // JPA maps this class to the "warehouse" table
@Getter @Setter                                  // Lombok generates all getters and setters
public class Warehouse {

    @Id                                          // Primary key column
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Long id;

    @Column(unique = true, nullable = false)      // Unique short code, e.g. "WH-001"
    private String code;

    @Column(nullable = false)                     // Human-readable display name
    private String name;
}
