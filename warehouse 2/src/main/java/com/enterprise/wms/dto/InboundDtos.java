package com.enterprise.wms.dto;

import java.time.LocalDate;

/**
 * DTOs for the inbound / goods-receipt workflow.
 */
public class InboundDtos {

    /**
     * Request body for receiving goods into the warehouse.
     *
     * @param barcode       product barcode to look up the SKU
     * @param warehouseCode target warehouse code (e.g. "WH-001")
     * @param lotNo         lot/batch number for traceability
     * @param expiryDate    expiry date of this batch (used for FEFO)
     * @param quantity      number of units received
     * @param grnNo         goods receipt note reference
     * @param performedBy   username of the person receiving
     */
    public record GoodsReceiptRequest(
            String barcode,
            String warehouseCode,
            String lotNo,
            LocalDate expiryDate,
            Integer quantity,
            String grnNo,
            String performedBy
    ) {}
}
