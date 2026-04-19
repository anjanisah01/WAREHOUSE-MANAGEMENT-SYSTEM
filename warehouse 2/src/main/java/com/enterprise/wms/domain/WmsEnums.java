package com.enterprise.wms.domain;

/**
 * All enumerations used across the WMS domain model.
 * Stored as strings in the database via @Enumerated(EnumType.STRING).
 */
public class WmsEnums {

    /** Physical type of a warehouse storage slot. */
    public enum LocationType { STORAGE, PICK_FACE, STAGING, RECEIVING, SHIPPING }

    /** Category of a stock movement event. */
    public enum MovementType { RECEIVE, PUTAWAY, PICK, REPLENISH, ADJUSTMENT, SHIP, TRANSFER_IN, TRANSFER_OUT }

    /** Lifecycle status of a sales order. CREATED → ALLOCATED → PICKED → PACKED → SHIPPED. */
    public enum OrderStatus { CREATED, PARTIALLY_ALLOCATED, ALLOCATED, PICKED, PACKED, SHIPPED }

    /** How items are picked: one order at a time, batched, or in a wave. */
    public enum PickingStrategy { SINGLE, BATCH, WAVE }

    /** Progress of a picking task. */
    public enum PickingTaskStatus { CREATED, IN_PROGRESS, COMPLETED }

    /** Type of system alert raised by the analytics rule engine. */
    public enum AlertType { LOW_STOCK, EXPIRY, DEAD_STOCK, REPLENISHMENT }

    /** Application security roles. */
    public enum RoleName { ADMIN, MANAGER, WORKER }

    /** Product movement frequency classification (for slotting optimisation). */
    public enum VelocityClass { FAST, MEDIUM, SLOW }

    /** Product weight class (affects storage location preference). */
    public enum WeightClass { LIGHT, MEDIUM, HEAVY }
}
