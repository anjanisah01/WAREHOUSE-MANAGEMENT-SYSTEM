package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.MovementType;
import com.enterprise.wms.domain.WmsEnums.VelocityClass;
import com.enterprise.wms.domain.WmsEnums.WeightClass;
import com.enterprise.wms.domain.entity.*;
import com.enterprise.wms.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Suggests optimal storage locations and executes putaway.
 * Uses configurable slotting scores that factor in product velocity and weight
 * to prefer certain zones/shelves (e.g. fast-movers near zone A, heavy items on low shelves).
 */
@Service
public class PutawayService {

    private final WarehouseRepository warehouseRepo;
    private final LocationRepository locationRepo;
    private final InventoryRepository inventoryRepo;
    private final MovementHistoryRepository moveRepo;
    private final ProductRepository productRepo;

    // ── Slotting configuration (from application.yml) ──
    @Value("${wms.slotting.fast-zone-prefix:A}")     private String  fastZonePrefix;   // zone prefix preferred for fast-movers
    @Value("${wms.slotting.fast-zone-bonus:100}")     private Integer fastZoneBonus;     // score bonus when fast product matches zone
    @Value("${wms.slotting.slow-zone-prefix:C}")      private String  slowZonePrefix;
    @Value("${wms.slotting.slow-zone-bonus:80}")      private Integer slowZoneBonus;
    @Value("${wms.slotting.heavy-shelf-suffix:1}")    private String  heavyShelfSuffix;  // shelf suffix preferred for heavy items
    @Value("${wms.slotting.heavy-shelf-bonus:120}")   private Integer heavyShelfBonus;
    @Value("${wms.slotting.light-shelf-suffix:3}")    private String  lightShelfSuffix;
    @Value("${wms.slotting.light-shelf-bonus:40}")    private Integer lightShelfBonus;

    public PutawayService(WarehouseRepository warehouseRepo, LocationRepository locationRepo,
                          InventoryRepository inventoryRepo, MovementHistoryRepository moveRepo,
                          ProductRepository productRepo) {
        this.warehouseRepo = warehouseRepo;
        this.locationRepo  = locationRepo;
        this.inventoryRepo = inventoryRepo;
        this.moveRepo      = moveRepo;
        this.productRepo   = productRepo;
    }

    /** Suggests the location with the least remaining capacity (simple mode — no product context). */
    public Location suggestStorageLocation(String warehouseCode) {
        Warehouse wh = warehouseRepo.findByCode(warehouseCode)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        return locationRepo.findByWarehouseAndOccupiedLessThan(wh, Integer.MAX_VALUE).stream()
                .filter(loc -> loc.getCapacity() != null && loc.getOccupied() != null
                            && loc.getCapacity() > loc.getOccupied())           // only non-full locations
                .min(Comparator.comparingInt(loc -> loc.getCapacity() - loc.getOccupied())) // tightest fit
                .orElseThrow(() -> new IllegalStateException("No available location"));
    }

    /** Suggests the best-scoring location for a specific product (uses slotting rules). */
    public Location suggestStorageLocation(String warehouseCode, Long productId) {
        Warehouse wh = warehouseRepo.findByCode(warehouseCode)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return locationRepo.findByWarehouseAndOccupiedLessThan(wh, Integer.MAX_VALUE).stream()
                .filter(loc -> loc.getCapacity() != null && loc.getOccupied() != null
                            && loc.getCapacity() > loc.getOccupied())
                .min(Comparator.comparingInt(loc -> slottingScore(loc, product))) // lowest score wins
                .orElseThrow(() -> new IllegalStateException("No slot available for product"));
    }

    /** Assigns inventory to a location, updates location occupancy, and logs a PUTAWAY movement. */
    @Transactional
    public Inventory executePutaway(Long inventoryId, Long locationId, Integer quantity, String performedBy) {
        Inventory inv = inventoryRepo.findById(inventoryId).orElseThrow();
        Location loc  = locationRepo.findById(locationId).orElseThrow();

        inv.setLocation(loc);                        // link inventory to the location
        inventoryRepo.save(inv);

        // Increase the location's occupied count
        loc.setOccupied((loc.getOccupied() == null ? 0 : loc.getOccupied()) + quantity);
        locationRepo.save(loc);

        // Record the PUTAWAY movement
        MovementHistory move = new MovementHistory();
        move.setProduct(inv.getProduct());
        move.setToLocation(loc);
        move.setQuantity(quantity);
        move.setMovementType(MovementType.PUTAWAY);
        move.setEventTime(LocalDateTime.now());
        move.setPerformedBy(performedBy);
        moveRepo.save(move);

        return inv;
    }

    /**
     * Calculates a slotting score for a (location, product) pair.
     * Lower score = better fit. Starts from remaining capacity, then subtracts bonuses
     * when the location's zone/shelf matches the product's velocity/weight class.
     */
    private int slottingScore(Location loc, Product product) {
        int score = loc.getCapacity() - loc.getOccupied(); // base = remaining capacity

        String zone  = (loc.getZone()  == null ? "" : loc.getZone()).toUpperCase();
        String shelf = (loc.getShelf() == null ? "" : loc.getShelf()).toUpperCase();
        String fp = (fastZonePrefix   == null ? "A" : fastZonePrefix).toUpperCase();
        String sp = (slowZonePrefix   == null ? "C" : slowZonePrefix).toUpperCase();
        String hs = (heavyShelfSuffix == null ? "1" : heavyShelfSuffix).toUpperCase();
        String ls = (lightShelfSuffix == null ? "3" : lightShelfSuffix).toUpperCase();

        // Bonus: fast-moving product + fast zone → lower score (preferred)
        if (product.getVelocityClass() == VelocityClass.FAST && zone.startsWith(fp))
            score -= (fastZoneBonus  == null ? 100 : fastZoneBonus);
        // Bonus: slow-moving product + slow zone
        if (product.getVelocityClass() == VelocityClass.SLOW && zone.startsWith(sp))
            score -= (slowZoneBonus  == null ? 80  : slowZoneBonus);
        // Bonus: heavy product + low shelf
        if (product.getWeightClass() == WeightClass.HEAVY && shelf.endsWith(hs))
            score -= (heavyShelfBonus == null ? 120 : heavyShelfBonus);
        // Bonus: light product + high shelf
        if (product.getWeightClass() == WeightClass.LIGHT && shelf.endsWith(ls))
            score -= (lightShelfBonus == null ? 40  : lightShelfBonus);

        return score;
    }
}
