package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.AlertType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A system-generated or rule-engine alert (low stock, expiry, dead stock, etc.).
 * Alerts stay open until manually or automatically resolved.
 */
@Entity
@Getter @Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)                 // LOW_STOCK, EXPIRY, DEAD_STOCK, REPLENISHMENT
    private AlertType alertType;

    private String message;                      // Human-readable description of the alert
    private String severity;                     // e.g. "HIGH", "MEDIUM", "LOW"
    private Boolean resolved;                    // True once the alert has been addressed
    private LocalDateTime createdAt;             // When the alert was raised
}
