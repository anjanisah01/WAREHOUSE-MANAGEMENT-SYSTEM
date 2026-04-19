package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A lot or batch number tied to a product.
 * Tracks expiry dates for FEFO (First-Expiry-First-Out) allocation.
 */
@Entity
@Getter @Setter
public class LotBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // The product this batch belongs to
    private Product product;

    @Column(nullable = false)                    // Lot/batch number string, e.g. "LOT-2024-001"
    private String lotNo;

    private LocalDate expiryDate;                // Expiry date (used for FEFO picking)
    private LocalDateTime receivedAt;            // Timestamp when this batch was received
}
