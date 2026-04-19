package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.MovementType;
import com.enterprise.wms.domain.entity.*;
import com.enterprise.wms.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Handles inter-warehouse stock transfers.
 * Picks the earliest-expiring source (FEFO), deducts from the source warehouse,
 * adds to the destination warehouse, and logs TRANSFER_OUT + TRANSFER_IN movements.
 */
@Service
public class TransferService {

    private final InventoryRepository inventoryRepo;
    private final MovementHistoryRepository moveRepo;
    private final ProductRepository productRepo;
    private final WarehouseRepository warehouseRepo;

    public TransferService(InventoryRepository inventoryRepo, MovementHistoryRepository moveRepo,
                           ProductRepository productRepo, WarehouseRepository warehouseRepo) {
        this.inventoryRepo = inventoryRepo;
        this.moveRepo      = moveRepo;
        this.productRepo   = productRepo;
        this.warehouseRepo = warehouseRepo;
    }

    /** Transfers qty units of a product from one warehouse to another. */
    @Transactional
    public Inventory transfer(String fromCode, String toCode, String sku, Integer qty, String performedBy) {
        // 1. Validate inputs
        if (qty == null || qty <= 0)
            throw new IllegalArgumentException("Transfer quantity must be greater than zero");
        if (fromCode == null || toCode == null || fromCode.equalsIgnoreCase(toCode))
            throw new IllegalArgumentException("Source and destination warehouses must be different");

        // 2. Look up product and both warehouses
        Product product   = productRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));
        Warehouse fromWh  = warehouseRepo.findByCode(fromCode).orElseThrow(() -> new IllegalArgumentException("Source warehouse not found: " + fromCode));
        Warehouse toWh    = warehouseRepo.findByCode(toCode).orElseThrow(() -> new IllegalArgumentException("Destination warehouse not found: " + toCode));

        // 3. Find a source inventory row with enough available (FEFO — earliest expiry first)
        Inventory source = inventoryRepo.findByProductAndWarehouseAndQuantityGreaterThan(product, fromWh, 0).stream()
                .filter(inv -> available(inv) >= qty)
                .min(Comparator.comparing(inv -> inv.getLotBatch() == null || inv.getLotBatch().getExpiryDate() == null
                        ? java.time.LocalDate.MAX : inv.getLotBatch().getExpiryDate()))
                .orElseThrow(() -> new IllegalArgumentException("Insufficient available quantity for SKU " + sku + " in " + fromCode));

        // 4. Deduct from source
        source.setQuantity(safe(source.getQuantity()) - qty);
        inventoryRepo.save(source);

        // 5. Upsert destination inventory
        Inventory dest = inventoryRepo.findByProductAndWarehouseAndLotBatch(product, toWh, source.getLotBatch())
                .orElseGet(Inventory::new);
        dest.setProduct(product);
        dest.setWarehouse(toWh);
        dest.setLotBatch(source.getLotBatch());
        dest.setLocation(dest.getLocation() == null ? source.getLocation() : dest.getLocation()); // inherit location if new
        dest.setReservedQty(safe(dest.getReservedQty()));
        dest.setQuantity(safe(dest.getQuantity()) + qty);
        Inventory savedDest = inventoryRepo.save(dest);

        // 6. Log two movement records (OUT + IN) with the same reference number
        String ref = "XFER-" + fromCode + "-" + toCode + "-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        String user = (performedBy == null || performedBy.isBlank()) ? "system" : performedBy;

        MovementHistory out = new MovementHistory();
        out.setProduct(product);
        out.setFromLocation(source.getLocation());       // where it left
        out.setQuantity(qty);
        out.setMovementType(MovementType.TRANSFER_OUT);
        out.setEventTime(now);
        out.setReferenceNo(ref);
        out.setPerformedBy(user);
        moveRepo.save(out);

        MovementHistory in = new MovementHistory();
        in.setProduct(product);
        in.setToLocation(savedDest.getLocation());       // where it arrived
        in.setQuantity(qty);
        in.setMovementType(MovementType.TRANSFER_IN);
        in.setEventTime(now);
        in.setReferenceNo(ref);
        in.setPerformedBy(user);
        moveRepo.save(in);

        return savedDest;
    }

    /** Available = on-hand minus reserved. */
    private static int available(Inventory inv) {
        return safe(inv.getQuantity()) - safe(inv.getReservedQty());
    }

    private static int safe(Integer v) { return v == null ? 0 : v; }
}
