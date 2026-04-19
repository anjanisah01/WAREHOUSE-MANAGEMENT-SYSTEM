package com.enterprise.wms.config;

import com.enterprise.wms.domain.WmsEnums.*;
import com.enterprise.wms.domain.entity.*;
import com.enterprise.wms.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds demo/reference data for local development.
 *
 * This runner is intentionally idempotent: each section checks existing data before inserting.
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final InventoryRepository inventoryRepository;
    private final LotBatchRepository lotBatchRepository;
    private final MovementHistoryRepository movementHistoryRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(WarehouseRepository warehouseRepository,
                           LocationRepository locationRepository,
                           ProductRepository productRepository,
                           AppUserRepository appUserRepository,
                           RoleRepository roleRepository,
                           InventoryRepository inventoryRepository,
                           LotBatchRepository lotBatchRepository,
                           MovementHistoryRepository movementHistoryRepository,
                           SalesOrderRepository salesOrderRepository,
                           SalesOrderLineRepository salesOrderLineRepository,
                           AlertRepository alertRepository,
                           PasswordEncoder passwordEncoder) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.inventoryRepository = inventoryRepository;
        this.lotBatchRepository = lotBatchRepository;
        this.movementHistoryRepository = movementHistoryRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderLineRepository = salesOrderLineRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // ─── Warehouses (5) ───
        Warehouse mum = ensureWarehouse("WH-MUM-01", "Mumbai Main Warehouse");
        Warehouse del = ensureWarehouse("WH-DEL-01", "Delhi Distribution Hub");
        Warehouse blr = ensureWarehouse("WH-BLR-01", "Bengaluru Automation Hub");
        Warehouse hyd = ensureWarehouse("WH-HYD-01", "Hyderabad Fulfilment Centre");
        Warehouse chn = ensureWarehouse("WH-CHN-01", "Chennai Port Warehouse");

        // ─── Locations (15) ───
        Location mumA1 = ensureLocation(mum, "A", "R1", "S1", "B1", 1000);
        Location mumA2 = ensureLocation(mum, "A", "R2", "S1", "B2", 800);
        Location mumB1 = ensureLocation(mum, "B", "R3", "S2", "B1", 600);
        Location mumC1 = ensureLocation(mum, "C", "R8", "S3", "B4", 800);
        Location delA1 = ensureLocation(del, "A", "R1", "S1", "B1", 900);
        Location delB1 = ensureLocation(del, "B", "R2", "S1", "B2", 700);
        Location delC1 = ensureLocation(del, "C", "R5", "S2", "B3", 500);
        Location blrA1 = ensureLocation(blr, "A", "R1", "S1", "B1", 1200);
        Location blrB1 = ensureLocation(blr, "B", "R3", "S1", "B2", 600);
        Location blrD1 = ensureLocation(blr, "D", "R4", "S2", "B6", 1200);
        Location hydA1 = ensureLocation(hyd, "A", "R1", "S1", "B1", 1000);
        Location hydB1 = ensureLocation(hyd, "B", "R2", "S2", "B1", 800);
        Location hydC1 = ensureLocation(hyd, "C", "R4", "S1", "B3", 500);
        Location chnA1 = ensureLocation(chn, "A", "R1", "S1", "B1", 1500);
        Location chnB1 = ensureLocation(chn, "B", "R3", "S2", "B2", 900);

        // ─── Products (24) ───
        Product bearings    = ensureProduct("SKU-1001", "Industrial Bearings 6205",        "8901000000011", 20,  VelocityClass.FAST,   WeightClass.HEAVY);
        Product hydraulic   = ensureProduct("SKU-1002", "Hydraulic Fluid ISO VG 46 (5L)",  "8901000000012", 15,  VelocityClass.MEDIUM, WeightClass.MEDIUM);
        Product bolts       = ensureProduct("SKU-1003", "Steel Bolts M12×50 (Box/100)",    "8901000000013", 100, VelocityClass.FAST,   WeightClass.MEDIUM);
        Product helmets     = ensureProduct("SKU-1004", "Safety Helmet EN 397",            "8901000000014", 25,  VelocityClass.MEDIUM, WeightClass.LIGHT);
        Product spray       = ensureProduct("SKU-1005", "WD-40 Lubricant Spray 400ml",    "8901000000015", 10,  VelocityClass.SLOW,   WeightClass.LIGHT);
        Product filters     = ensureProduct("SKU-1006", "HEPA Filter Cartridge H13",      "8901000000016", 30,  VelocityClass.MEDIUM, WeightClass.LIGHT);
        Product gloves      = ensureProduct("SKU-1007", "Nitrile Gloves (Box/200)",        "8901000000017", 50,  VelocityClass.FAST,   WeightClass.LIGHT);
        Product motorOil    = ensureProduct("SKU-1008", "Castrol GTX 15W-40 (20L)",       "8901000000018", 12,  VelocityClass.MEDIUM, WeightClass.HEAVY);
        Product welding     = ensureProduct("SKU-1009", "Welding Electrodes E6013 (5kg)",  "8901000000019", 40,  VelocityClass.FAST,   WeightClass.MEDIUM);
        Product cable       = ensureProduct("SKU-1010", "Copper Cable 2.5mm (100m Roll)",  "8901000000020", 8,   VelocityClass.MEDIUM, WeightClass.HEAVY);
        Product sandpaper   = ensureProduct("SKU-1011", "Sandpaper P120 (Pack/50)",        "8901000000021", 60,  VelocityClass.FAST,   WeightClass.LIGHT);
        Product epoxy       = ensureProduct("SKU-1012", "Araldite Epoxy Resin 1kg",       "8901000000022", 15,  VelocityClass.SLOW,   WeightClass.MEDIUM);
        Product chainHoist  = ensureProduct("SKU-1013", "Chain Hoist 2-Ton",               "8901000000023", 3,   VelocityClass.SLOW,   WeightClass.HEAVY);
        Product drill       = ensureProduct("SKU-1014", "Bosch GSB 550 Drill",             "8901000000024", 5,   VelocityClass.MEDIUM, WeightClass.MEDIUM);
        Product safetyVest  = ensureProduct("SKU-1015", "Hi-Vis Safety Vest (Pack/10)",    "8901000000025", 30,  VelocityClass.FAST,   WeightClass.LIGHT);
        Product pvcPipe     = ensureProduct("SKU-1016", "PVC Pipe 4\" (6m length)",        "8901000000026", 20,  VelocityClass.MEDIUM, WeightClass.HEAVY);
        Product cementBag   = ensureProduct("SKU-1017", "Portland Cement 50kg",            "8901000000027", 50,  VelocityClass.FAST,   WeightClass.HEAVY);
        Product wireRope    = ensureProduct("SKU-1018", "Wire Rope 12mm (50m)",            "8901000000028", 6,   VelocityClass.SLOW,   WeightClass.HEAVY);
        Product tapeMeasure = ensureProduct("SKU-1019", "Stanley Tape Measure 8m",         "8901000000029", 20,  VelocityClass.MEDIUM, WeightClass.LIGHT);
        Product sprayPaint  = ensureProduct("SKU-1020", "Aerosol Spray Paint RAL 9010",    "8901000000030", 25,  VelocityClass.MEDIUM, WeightClass.LIGHT);
        Product steelPlate  = ensureProduct("SKU-1021", "Steel Plate 3mm (1200×2400)",     "8901000000031", 4,   VelocityClass.SLOW,   WeightClass.HEAVY);
        Product fireExt     = ensureProduct("SKU-1022", "ABC Fire Extinguisher 6kg",       "8901000000032", 10,  VelocityClass.SLOW,   WeightClass.MEDIUM);
        Product masks       = ensureProduct("SKU-1023", "FFP2 Dust Mask (Box/50)",         "8901000000033", 40,  VelocityClass.FAST,   WeightClass.LIGHT);
        Product sealant     = ensureProduct("SKU-1024", "Silicone Sealant 310ml",          "8901000000034", 30,  VelocityClass.MEDIUM, WeightClass.LIGHT);

        // ─── Inventory & Movements (seed only once) ───
        LocalDateTime now = LocalDateTime.now();
        if (inventoryRepository.count() == 0) {

            // Mumbai warehouse (diverse stock)
            seedInventory(bearings,   mum, mumA1,  45,  20,  90,  5,  MovementType.RECEIVE,   "worker1",  now.minusHours(2));
            seedInventory(hydraulic,  mum, mumA2,  8,   15,  22,  2,  MovementType.RECEIVE,   "worker1",  now.minusHours(5));
            seedInventory(bolts,      mum, mumA1,  520, 100, 365, 20, MovementType.RECEIVE,   "worker2",  now.minusDays(1));
            seedInventory(helmets,    mum, mumB1,  18,  25,  180, 3,  MovementType.PICK,      "worker1",  now.minusHours(8));
            seedInventory(gloves,     mum, mumB1,  200, 50,  120, 10, MovementType.RECEIVE,   "worker2",  now.minusHours(3));
            seedInventory(sandpaper,  mum, mumC1,  150, 60,  730, 0,  MovementType.REPLENISH, "worker1",  now.minusDays(2));
            seedInventory(safetyVest, mum, mumC1,  90,  30,  365, 5,  MovementType.RECEIVE,   "worker2",  now.minusHours(12));
            seedInventory(masks,      mum, mumA2,  120, 40,  60,  8,  MovementType.RECEIVE,   "manager1", now.minusHours(1));

            // Delhi warehouse (construction & heavy goods)
            seedInventory(cementBag,  del, delA1,  180, 50,  90,  30, MovementType.RECEIVE,   "worker1",  now.minusHours(4));
            seedInventory(pvcPipe,    del, delA1,  35,  20,  730, 0,  MovementType.RECEIVE,   "worker2",  now.minusDays(3));
            seedInventory(steelPlate, del, delB1,  6,   4,   730, 0,  MovementType.RECEIVE,   "worker1",  now.minusDays(5));
            seedInventory(wireRope,   del, delB1,  4,   6,   365, 0,  MovementType.RECEIVE,   "worker2",  now.minusDays(2));
            seedInventory(welding,    del, delC1,  110, 40,  180, 12, MovementType.PICK,      "worker1",  now.minusHours(6));
            seedInventory(bolts,      del, delC1,  280, 100, 365, 15, MovementType.TRANSFER_IN, "worker2",  now.minusDays(1));
            seedInventory(helmets,    del, delA1,  5,   25,  45,  2,  MovementType.SHIP,      "worker1",  now.minusHours(10));

            // Bangalore warehouse (automation & electronics)
            seedInventory(cable,      blr, blrA1,  22,  8,   365, 0,  MovementType.RECEIVE,   "worker1",  now.minusDays(4));
            seedInventory(drill,      blr, blrA1,  14,  5,   365, 0,  MovementType.RECEIVE,   "worker2",  now.minusHours(7));
            seedInventory(motorOil,   blr, blrB1,  28,  12,  180, 4,  MovementType.RECEIVE,   "worker1",  now.minusHours(3));
            seedInventory(filters,    blr, blrD1,  55,  30,  45,  5,  MovementType.RECEIVE,   "worker2",  now.minusDays(1));
            seedInventory(spray,      blr, blrD1,  3,   10,  8,   0,  MovementType.REPLENISH, "worker1",  now.minusHours(14));
            seedInventory(epoxy,      blr, blrB1,  7,   15,  60,  2,  MovementType.PICK,      "worker2",  now.minusHours(9));

            // Hyderabad warehouse (safety & consumables)
            seedInventory(fireExt,    hyd, hydA1,  18,  10,  730, 0,  MovementType.RECEIVE,   "worker1",  now.minusDays(6));
            seedInventory(gloves,     hyd, hydA1,  85,  50,  90,  6,  MovementType.RECEIVE,   "worker2",  now.minusHours(2));
            seedInventory(masks,      hyd, hydB1,  30,  40,  15,  4,  MovementType.PICK,      "worker1",  now.minusHours(5));
            seedInventory(sealant,    hyd, hydB1,  45,  30,  180, 3,  MovementType.RECEIVE,   "worker2",  now.minusDays(1));
            seedInventory(tapeMeasure,hyd, hydC1,  60,  20,  730, 0,  MovementType.RECEIVE,   "worker1",  now.minusHours(8));
            seedInventory(sprayPaint, hyd, hydC1,  18,  25,  365, 2,  MovementType.RECEIVE,   "worker2",  now.minusDays(2));

            // Chennai warehouse (port-heavy goods)
            seedInventory(steelPlate, chn, chnA1,  12,  4,   730, 0,  MovementType.RECEIVE,   "worker1",  now.minusDays(7));
            seedInventory(chainHoist, chn, chnA1,  2,   3,   365, 0,  MovementType.RECEIVE,   "worker2",  now.minusDays(3));
            seedInventory(cementBag,  chn, chnB1,  300, 50,  90,  40, MovementType.RECEIVE,   "worker1",  now.minusDays(1));
            seedInventory(pvcPipe,    chn, chnB1,  60,  20,  730, 0,  MovementType.RECEIVE,   "worker2",  now.minusHours(6));
            seedInventory(bearings,   chn, chnA1,  10,  20,  120, 3,  MovementType.TRANSFER_IN, "worker1",  now.minusHours(4));

            // Additional movement history for realism
            addMovement(bolts,   mumA1, delC1, 50,  MovementType.TRANSFER_OUT, "GRN-TRF-001", "manager1", now.minusHours(16));
            addMovement(gloves,  mumB1, null,  30,  MovementType.PICK,      "SO-2025-003",  "worker1",  now.minusHours(4));
            addMovement(helmets, delA1, null,  5,   MovementType.SHIP,      "SO-2025-005",  "worker2",  now.minusHours(3));
            addMovement(cable,   null,  blrA1, 10,  MovementType.RECEIVE,   "GRN-2025-012", "worker1",  now.minusHours(7));
            addMovement(masks,   hydB1, null,  15,  MovementType.PICK,      "SO-2025-008",  "worker1",  now.minusHours(2));
            addMovement(cementBag,chnB1,null,  80,  MovementType.SHIP,      "SO-2025-010",  "worker2",  now.minusHours(1));
            addMovement(bearings, mumA1,chnA1, 8,   MovementType.TRANSFER_OUT, "TRF-2025-002", "manager1", now.minusDays(1).minusHours(3));
            addMovement(sealant,  null, hydB1, 20,  MovementType.RECEIVE,   "GRN-2025-014", "worker2",  now.minusHours(9));
            addMovement(sandpaper,mumC1,null,  25,  MovementType.PICK,      "SO-2025-012",  "worker1",  now.minusDays(1));
            addMovement(welding,  delC1,null,  20,  MovementType.PICK,      "SO-2025-015",  "worker2",  now.minusHours(11));
        }

        // ─── Sales Orders ───
        if (salesOrderRepository.count() == 0) {
            createOrder("SO-2025-001", mum, OrderStatus.SHIPPED,  now.minusDays(5), bearings, 10, 10, bolts, 50, 50);
            createOrder("SO-2025-002", del, OrderStatus.PICKED,  now.minusDays(2), welding, 30, 20, helmets, 10, 5);
            createOrder("SO-2025-003", mum, OrderStatus.ALLOCATED,now.minusDays(1), gloves, 60, 60, masks, 40, 40);
            createOrder("SO-2025-004", blr, OrderStatus.CREATED,  now.minusHours(6),cable, 5, 0, drill, 3, 0);
            createOrder("SO-2025-005", hyd, OrderStatus.PACKED,   now.minusDays(3), fireExt, 4, 4, sealant, 15, 15);
            createOrder("SO-2025-006", chn, OrderStatus.SHIPPED,  now.minusDays(7), cementBag, 100, 100, steelPlate, 4, 4);
            createOrder("SO-2025-007", del, OrderStatus.ALLOCATED, now.minusHours(12),pvcPipe, 15, 10, cementBag, 40, 30);
        }

        // ─── Alerts ───
        if (alertRepository.count() == 0) {
            addAlert(AlertType.LOW_STOCK, "LOW_STOCK: Safety Helmets at WH-MUM-01 — Qty 18 below reorder level 25", "HIGH");
            addAlert(AlertType.LOW_STOCK, "LOW_STOCK: Hydraulic Fluid at WH-MUM-01 — Qty 8 below reorder level 15", "HIGH");
            addAlert(AlertType.LOW_STOCK, "LOW_STOCK: Wire Rope at WH-DEL-01 — Qty 4 below reorder level 6", "MEDIUM");
            addAlert(AlertType.LOW_STOCK, "LOW_STOCK: Chain Hoist at WH-CHN-01 — Qty 2 below reorder level 3", "MEDIUM");
            addAlert(AlertType.EXPIRY,    "EXPIRY: WD-40 Lubricant Spray at WH-BLR-01 — expires in 8 days", "HIGH");
            addAlert(AlertType.EXPIRY,    "EXPIRY: FFP2 Dust Masks at WH-HYD-01 — expires in 15 days", "MEDIUM");
            addAlert(AlertType.EXPIRY,    "EXPIRY: Helmets at WH-DEL-01 — lot expires in 45 days (FEFO review)", "LOW");
            addAlert(AlertType.REPLENISHMENT, "REPLENISHMENT: Spray Paint at WH-HYD-01 — Qty 18 approaching reorder level 25", "LOW");
        }

        // ─── Users ───
        if (appUserRepository.count() == 0) {
            Role adminRole   = ensureRole(RoleName.ADMIN);
            Role managerRole = ensureRole(RoleName.MANAGER);
            Role workerRole  = ensureRole(RoleName.WORKER);

            createUser("admin",    "admin123",   adminRole, managerRole, workerRole);
            createUser("manager1", "manager123", managerRole, workerRole);
            createUser("manager2", "manager123", managerRole, workerRole);
            createUser("worker1",  "worker123",  workerRole);
            createUser("worker2",  "worker123",  workerRole);
            createUser("worker3",  "worker123",  workerRole);
        }
    }

    // ─── Helper methods ───

    /** Find or create a warehouse by its unique code. */
    private Warehouse ensureWarehouse(String code, String name) {
        return warehouseRepository.findByCode(code).orElseGet(() -> {
            Warehouse w = new Warehouse();       // Not found → create new
            w.setCode(code);
            w.setName(name);
            return warehouseRepository.save(w);  // Persist and return
        });
    }

    /** Find or create a storage location (matched on warehouse + zone/rack/shelf/bin). */
    private Location ensureLocation(Warehouse warehouse, String zone, String rack, String shelf, String bin, int capacity) {
        return locationRepository.findAll().stream()
                .filter(l -> l.getWarehouse() != null
                        && l.getWarehouse().getId().equals(warehouse.getId())
                        && zone.equals(l.getZone()) && rack.equals(l.getRack())
                        && shelf.equals(l.getShelf()) && bin.equals(l.getBin()))
                .findFirst()
                .orElseGet(() -> {
                    Location l = new Location(); // Not found → create new
                    l.setWarehouse(warehouse);
                    l.setZone(zone);
                    l.setRack(rack);
                    l.setShelf(shelf);
                    l.setBin(bin);
                    l.setLocationType(LocationType.STORAGE); // Default type
                    l.setCapacity(capacity);
                    l.setOccupied(0);            // Start empty
                    return locationRepository.save(l);
                });
    }

    /** Find or create a product by its SKU. */
    private Product ensureProduct(String sku, String name, String barcode, int reorderLevel,
                                  VelocityClass velocityClass, WeightClass weightClass) {
        return productRepository.findBySku(sku).orElseGet(() -> {
            Product p = new Product();
            p.setSku(sku);
            p.setName(name);
            p.setBarcode(barcode);
            p.setReorderLevel(reorderLevel);     // Alert threshold
            p.setVelocityClass(velocityClass);   // Movement frequency
            p.setWeightClass(weightClass);       // Affects slotting
            return productRepository.save(p);
        });
    }

    /**
     * Create an inventory row + lot/batch + initial movement record for one product.
     * Combines three steps: update reorder level, create lot, create inventory, log movement.
     */
    private void seedInventory(Product product, Warehouse warehouse, Location location,
                               int qty, int reorderLevel, int expiryDays, int reserved,
                               MovementType movementType, String performedBy, LocalDateTime eventTime) {
        product.setReorderLevel(reorderLevel);   // Override reorder level per warehouse context
        productRepository.save(product);

        LotBatch lot = new LotBatch();           // Create a batch for this stock row
        lot.setProduct(product);
        lot.setLotNo("LOT-" + product.getSku() + "-" + warehouse.getCode().substring(3, 6)); // e.g. LOT-SKU-1001-MUM
        lot.setExpiryDate(LocalDate.now().plusDays(expiryDays)); // Days until expiry
        lot.setReceivedAt(eventTime.minusDays(2));              // Pretend received 2 days before event
        lotBatchRepository.save(lot);

        Inventory inv = new Inventory();         // Create the on-hand stock record
        inv.setProduct(product);
        inv.setWarehouse(warehouse);
        inv.setLocation(location);
        inv.setLotBatch(lot);
        inv.setQuantity(qty);                    // Available units
        inv.setReservedQty(reserved);            // Units already reserved by orders
        inventoryRepository.save(inv);

        // Log the initial movement that brought this stock in
        addMovement(product, null, location, Math.max(1, Math.min(qty, 50)),
                    movementType, "INIT-" + product.getSku(), performedBy, eventTime);
    }

    /** Record a single stock movement in the movement_history table. */
    private void addMovement(Product product, Location from, Location to, int qty,
                             MovementType type, String ref, String performedBy, LocalDateTime time) {
        MovementHistory h = new MovementHistory();
        h.setProduct(product);
        h.setFromLocation(from);                 // Null for inbound receives
        h.setToLocation(to);                     // Null for outbound ships
        h.setQuantity(qty);
        h.setMovementType(type);
        h.setReferenceNo(ref);                   // GRN or order reference
        h.setPerformedBy(performedBy);
        h.setEventTime(time);
        movementHistoryRepository.save(h);
    }

    /** Create a sales order with exactly two line items. */
    private void createOrder(String orderNo, Warehouse wh, OrderStatus status, LocalDateTime created,
                             Product p1, int req1, int alloc1, Product p2, int req2, int alloc2) {
        SalesOrder o = new SalesOrder();
        o.setOrderNo(orderNo);
        o.setWarehouse(wh);
        o.setStatus(status);
        o.setCreatedAt(created);
        salesOrderRepository.save(o);            // Save header first (FK needed for lines)

        SalesOrderLine l1 = new SalesOrderLine();// First line item
        l1.setOrderRef(o);
        l1.setProduct(p1);
        l1.setRequestedQty(req1);
        l1.setAllocatedQty(alloc1);
        salesOrderLineRepository.save(l1);

        SalesOrderLine l2 = new SalesOrderLine();// Second line item
        l2.setOrderRef(o);
        l2.setProduct(p2);
        l2.setRequestedQty(req2);
        l2.setAllocatedQty(alloc2);
        salesOrderLineRepository.save(l2);
    }

    /** Create an unresolved alert with the given type, message, and severity. */
    private void addAlert(AlertType type, String message, String severity) {
        Alert a = new Alert();
        a.setAlertType(type);
        a.setMessage(message);
        a.setSeverity(severity);
        a.setResolved(false);                    // New alerts start unresolved
        a.setCreatedAt(LocalDateTime.now());
        alertRepository.save(a);
    }

    /** Find or create a security role by its enum name. */
    private Role ensureRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();                 // Role doesn't exist yet → create it
            r.setName(name);
            return roleRepository.save(r);
        });
    }

    /** Create a user with BCrypt-hashed password and the given roles. */
    private void createUser(String username, String password, Role... roles) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(password)); // Hash the plain-text password
        u.setRoles(new HashSet<>(Arrays.asList(roles)));     // Assign all provided roles
        appUserRepository.save(u);
    }
}
