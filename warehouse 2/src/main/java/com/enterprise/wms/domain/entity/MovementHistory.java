package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.MovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable audit trail for every stock movement (receive, pick, transfer, etc.).
 * Each row captures what moved, where, when, and who performed it.
 */
@Entity
@Getter @Setter
public class MovementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // Which product moved
    private Product product;

    @ManyToOne                                   // Origin slot (null for inbound receives)
    private Location fromLocation;

    @ManyToOne                                   // Destination slot (null for outbound ships)
    private Location toLocation;

    private Integer quantity;                    // Units moved

    @Enumerated(EnumType.STRING)                 // RECEIVE, PUTAWAY, PICK, SHIP, etc.
    private MovementType movementType;

    private LocalDateTime eventTime;             // When the movement occurred
    private String referenceNo;                  // GRN or order reference
    private String performedBy;                  // Username who performed the action
}
