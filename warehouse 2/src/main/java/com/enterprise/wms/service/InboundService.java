package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.MovementType;
import com.enterprise.wms.domain.entity.*;
import com.enterprise.wms.dto.InboundDtos.GoodsReceiptRequest;
import com.enterprise.wms.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles inbound goods receiving (GRN).
 * Creates a lot/batch record, upserts inventory, and logs the movement.
 */
@Service
public class InboundService {

    private final ProductRepository productRepo;    // looks up products
    private final WarehouseRepository warehouseRepo; // looks up warehouses
    private final LotBatchRepository lotRepo;        // persists lot/batch records
    private final InventoryRepository inventoryRepo; // reads/writes stock levels
    private final MovementHistoryRepository moveRepo; // logs stock movements

    public InboundService(ProductRepository productRepo,
                          WarehouseRepository warehouseRepo,
                          LotBatchRepository lotRepo,
                          InventoryRepository inventoryRepo,
                          MovementHistoryRepository moveRepo) {
        this.productRepo = productRepo;
        this.warehouseRepo = warehouseRepo;
        this.lotRepo = lotRepo;
        this.inventoryRepo = inventoryRepo;
        this.moveRepo = moveRepo;
    }

    /**
     * Receives goods into the warehouse.
     * Steps: validate barcode + warehouse → create lot → upsert inventory → log movement.
     */
    @Transactional
    public Inventory receiveGoods(GoodsReceiptRequest request) {
        // 1. Look up the product by its barcode
        Product product = productRepo.findByBarcode(request.barcode())
                .orElseThrow(() -> new IllegalArgumentException("Barcode not found: " + request.barcode()));

        // 2. Look up the destination warehouse
        Warehouse warehouse = warehouseRepo.findByCode(request.warehouseCode())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + request.warehouseCode()));

        // 3. Create a new lot/batch record for this receipt
        LotBatch lot = new LotBatch();
        lot.setProduct(product);                    // which product this lot contains
        lot.setLotNo(request.lotNo());              // supplier lot number
        lot.setExpiryDate(request.expiryDate());    // when it expires
        lot.setReceivedAt(LocalDateTime.now());     // timestamp of receipt
        lotRepo.save(lot);

        // 4. Upsert inventory: find existing row or create a new one
        Inventory inv = inventoryRepo.findByProductAndWarehouseAndLotBatch(product, warehouse, lot)
                .orElseGet(Inventory::new);          // new row if nothing found
        inv.setProduct(product);
        inv.setWarehouse(warehouse);
        inv.setLotBatch(lot);
        // Add the received quantity to any existing stock
        inv.setQuantity(safe(inv.getQuantity()) + request.quantity());
        inv.setReservedQty(safe(inv.getReservedQty())); // ensure reserved is non-null
        inventoryRepo.save(inv);

        // 5. Record the movement for audit/reporting
        MovementHistory move = new MovementHistory();
        move.setProduct(product);
        move.setQuantity(request.quantity());        // how many units arrived
        move.setMovementType(MovementType.RECEIVE);  // inbound movement type
        move.setEventTime(LocalDateTime.now());
        move.setReferenceNo(request.grnNo());        // GRN (goods receipt note) number
        move.setPerformedBy(request.performedBy());   // who performed the receipt
        moveRepo.save(move);

        return inv; // return the updated inventory record
    }

    /** Null-safe integer helper. */
    private static int safe(Integer v) { return v == null ? 0 : v; }
}
