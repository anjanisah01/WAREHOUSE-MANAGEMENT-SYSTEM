package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.LocationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single storage slot inside a warehouse, identified by zone/rack/shelf/bin.
 * The combination (warehouse + zone + rack + shelf + bin) is unique.
 */
@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "zone", "rack", "shelf", "bin"}))
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // Which warehouse this slot belongs to
    private Warehouse warehouse;

    @Column(nullable = false)                    // Zone label, e.g. "A", "B"
    private String zone;

    @Column(nullable = false)                    // Rack identifier within the zone
    private String rack;

    @Column(nullable = false)                    // Shelf level on the rack
    private String shelf;

    @Column(nullable = false)                    // Bin position on the shelf
    private String bin;

    @Enumerated(EnumType.STRING)                 // STORAGE, PICK_FACE, STAGING, etc.
    private LocationType locationType;

    private Integer capacity;                    // Max units this slot can hold
    private Integer occupied;                    // Units currently stored here
}
