package com.enterprise.wms.dto;

import java.util.List;

/**
 * DTOs for creating sales orders and planning wave picks.
 */
public class OrderDtos {

    /** A single line item: product SKU + quantity. */
    public record CreateOrderLineRequest(String sku, Integer quantity) {}

    /** Request to create a new sales order with one or more line items. */
    public record CreateOrderRequest(String orderNo, String warehouseCode, List<CreateOrderLineRequest> lines) {}

    /**
     * Request to plan a wave pick across multiple orders.
     *
     * @param warehouseCode  warehouse to pick from
     * @param waveNo         wave identifier
     * @param orderIds       list of order IDs included in the wave
     * @param workerUsername worker assigned to the wave
     */
    public record WavePlanRequest(String warehouseCode, String waveNo, List<Long> orderIds, String workerUsername) {}
}
