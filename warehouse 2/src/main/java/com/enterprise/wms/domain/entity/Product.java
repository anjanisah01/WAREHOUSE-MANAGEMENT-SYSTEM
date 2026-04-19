package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.VelocityClass;
import com.enterprise.wms.domain.WmsEnums.WeightClass;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A stock-keeping unit (SKU) in the catalogue.
 * Products are referenced by inventory, orders, and lot/batch records.
 */
@Entity
@Getter @Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)      // Internal product code, e.g. "SKU-1001"
    private String sku;

    @Column(nullable = false)                     // Display name, e.g. "Widget X"
    private String name;

    @Column(unique = true, nullable = false)      // Scannable barcode string
    private String barcode;

    private Integer reorderLevel;                 // Stock threshold for low-stock alerts

    @Enumerated(EnumType.STRING)                  // FAST / MEDIUM / SLOW — movement frequency
    private VelocityClass velocityClass;

    @Enumerated(EnumType.STRING)                  // LIGHT / MEDIUM / HEAVY — affects slotting
    private WeightClass weightClass;
}
