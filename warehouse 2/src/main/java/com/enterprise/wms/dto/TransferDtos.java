package com.enterprise.wms.dto;

/**
 * DTOs for inter-warehouse stock transfers.
 */
public class TransferDtos {

    /**
     * Request to transfer stock between warehouses using FEFO allocation.
     *
     * @param fromWarehouseCode source warehouse code
     * @param toWarehouseCode   destination warehouse code
     * @param sku               product SKU to transfer
     * @param quantity          number of units to move
     * @param performedBy       username performing the transfer
     */
    public record TransferRequest(String fromWarehouseCode, String toWarehouseCode, String sku, Integer quantity, String performedBy) {}
}
