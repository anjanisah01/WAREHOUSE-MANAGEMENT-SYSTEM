package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.OrderStatus;
import com.enterprise.wms.domain.WmsEnums.PickingStrategy;
import com.enterprise.wms.domain.WmsEnums.PickingTaskStatus;
import com.enterprise.wms.domain.entity.*;
import com.enterprise.wms.dto.OrderDtos.CreateOrderRequest;
import com.enterprise.wms.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the outbound (order fulfilment) pipeline:
 *   create sales orders → allocate stock (FEFO) → create picking tasks → update status → ship.
 */
@Service
public class OutboundService {

    private static final Logger log = LoggerFactory.getLogger(OutboundService.class);

    private final SalesOrderRepository orderRepo;         // persists sales order headers
    private final SalesOrderLineRepository lineRepo;      // persists order line items
    private final PickingTaskRepository pickingRepo;      // persists picking tasks
    private final ProductRepository productRepo;          // looks up products by SKU
    private final WarehouseRepository warehouseRepo;      // looks up warehouses by code
    private final InventoryRepository inventoryRepo;      // reads/writes stock levels

    public OutboundService(SalesOrderRepository orderRepo,
                           SalesOrderLineRepository lineRepo,
                           PickingTaskRepository pickingRepo,
                           ProductRepository productRepo,
                           WarehouseRepository warehouseRepo,
                           InventoryRepository inventoryRepo) {
        this.orderRepo = orderRepo;
        this.lineRepo = lineRepo;
        this.pickingRepo = pickingRepo;
        this.productRepo = productRepo;
        this.warehouseRepo = warehouseRepo;
        this.inventoryRepo = inventoryRepo;
    }

    /* ── Create a new sales order with its line items ── */

    @Transactional
    public SalesOrder createOrder(CreateOrderRequest request) {
        // Look up the warehouse by its code
        Warehouse warehouse = warehouseRepo.findByCode(request.warehouseCode())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        // Build and persist the order header
        SalesOrder order = new SalesOrder();
        order.setOrderNo(request.orderNo());       // unique order number
        order.setWarehouse(warehouse);              // which warehouse fulfils it
        order.setStatus(OrderStatus.CREATED);       // starts in CREATED state
        order.setCreatedAt(LocalDateTime.now());    // timestamp
        SalesOrder saved = orderRepo.save(order);

        // Create one line per requested SKU + quantity
        for (var line : request.lines()) {
            SalesOrderLine ol = new SalesOrderLine();
            ol.setOrderRef(saved);                  // link back to parent order
            ol.setProduct(productRepo.findBySku(line.sku())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + line.sku())));
            ol.setRequestedQty(line.quantity());    // how many the customer wants
            ol.setAllocatedQty(0);                  // nothing allocated yet
            lineRepo.save(ol);
        }
        return saved;
    }

    /* ── Create a single picking task for one order ── */

    @Transactional
    public PickingTask createPickingTask(Long orderId, PickingStrategy strategy, String workerUsername) {
        SalesOrder order = orderRepo.findById(orderId).orElseThrow();
        return buildTask(order, strategy, workerUsername, null); // no wave number for single picks
    }

    /* ── Wave picking: batch multiple orders into one wave ── */

    @Transactional
    public List<PickingTask> createWavePickingTasks(String warehouseCode, String waveNo,
                                                    List<Long> orderIds, String workerUsername) {
        Warehouse warehouse = warehouseRepo.findByCode(warehouseCode)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        List<PickingTask> tasks = new ArrayList<>();
        for (Long orderId : orderIds) {
            SalesOrder order = orderRepo.findById(orderId).orElseThrow();
            // All orders in a wave must belong to the same warehouse
            if (!order.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Order " + order.getOrderNo() + " belongs to another warehouse");
            }
            tasks.add(buildTask(order, PickingStrategy.WAVE, workerUsername, waveNo));
        }
        return tasks;
    }

    /* ── Read-only queries ── */

    /** Returns all picking tasks, newest first. */
    public List<PickingTask> activePickingTasks() {
        return pickingRepo.findAllByOrderByCreatedAtDesc();
    }

    /** Returns every sales order. */
    public List<SalesOrder> listOrders() {
        return orderRepo.findAll();
    }

    /* ── Update picking progress / status ── */

    @Transactional
    public PickingTask updatePickingStatus(Long taskId, PickingTaskStatus newStatus, Integer progressPct) {
        if (newStatus == null) throw new IllegalArgumentException("Picking task status is required");

        // Load the task and verify the state-machine transition is valid
        PickingTask task = pickingRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Picking task not found: " + taskId));
        validateTransition(task.getStatus(), newStatus);

        task.setStatus(newStatus);

        if (newStatus == PickingTaskStatus.COMPLETED) {
            // Completion → 100% progress and mark the order as PICKED
            task.setPickingProgressPct(100);
            task.getOrderRef().setStatus(OrderStatus.PICKED);
            orderRepo.save(task.getOrderRef());
        } else if (newStatus == PickingTaskStatus.IN_PROGRESS) {
            // Clamp the caller-provided progress between 0-99 (100 is reserved for COMPLETED)
            int current = safe(task.getPickingProgressPct());
            task.setPickingProgressPct(Math.max(0, Math.min(99, progressPct != null ? progressPct : current)));
        } else {
            task.setPickingProgressPct(0); // reset for any other status
        }

        return pickingRepo.save(task);
    }

    /* ── Mark an order as shipped ── */

    @Transactional
    public SalesOrder markShipped(Long orderId) {
        SalesOrder order = orderRepo.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);       // final outbound state
        return orderRepo.save(order);
    }

    // ────────────────────── Private helpers ──────────────────────

    /** Allocates stock with FEFO, creates a picking task, and persists both. */
    private PickingTask buildTask(SalesOrder order, PickingStrategy strategy,
                                  String workerUsername, String waveNo) {
        // Run FEFO allocation and update order status accordingly
        boolean full = allocateFefo(order);
        order.setStatus(full ? OrderStatus.ALLOCATED : OrderStatus.PARTIALLY_ALLOCATED);
        orderRepo.save(order);

        // Create the picking task linked to this order
        PickingTask task = new PickingTask();
        task.setOrderRef(order);
        task.setStrategy(strategy);
        task.setWorkerUsername(workerUsername);
        task.setWaveNo(waveNo);
        task.setStatus(PickingTaskStatus.CREATED);
        task.setPickingProgressPct(0);
        task.setCreatedAt(LocalDateTime.now());
        return pickingRepo.save(task);
    }

    /**
     * First-Expiry-First-Out allocation: reserves inventory starting from the lot that expires soonest.
     * Returns true only when every line is fully allocated.
     */
    private boolean allocateFefo(SalesOrder order) {
        boolean fullyAllocated = true;

        for (SalesOrderLine line : lineRepo.findByOrderRef(order)) {
            int requested  = safe(line.getRequestedQty());
            int allocated  = safe(line.getAllocatedQty());
            int remaining  = Math.max(0, requested - allocated); // how much still needs allocating

            // Fetch inventory pools for this product+warehouse that have stock > 0
            List<Inventory> pools = inventoryRepo
                    .findByProductAndWarehouseAndQuantityGreaterThan(line.getProduct(), order.getWarehouse(), 0);

            // Sort by expiry ascending (earliest first) — the FEFO rule
            pools.sort(Comparator.comparing(inv ->
                    inv.getLotBatch() == null || inv.getLotBatch().getExpiryDate() == null
                            ? LocalDate.MAX : inv.getLotBatch().getExpiryDate()));

            // Reserve from each pool until the line is satisfied
            for (Inventory inv : pools) {
                if (remaining <= 0) break;
                int available = safe(inv.getQuantity()) - safe(inv.getReservedQty());
                if (available <= 0) continue;

                int pick = Math.min(available, remaining);                 // take what we can
                inv.setReservedQty(safe(inv.getReservedQty()) + pick);     // mark as reserved
                inventoryRepo.save(inv);
                allocated += pick;
                remaining -= pick;
            }

            line.setAllocatedQty(allocated);
            lineRepo.save(line);

            if (allocated < requested) {
                fullyAllocated = false;
                log.warn("Partial FEFO for order {} / SKU {}: {} of {}",
                        order.getOrderNo(), line.getProduct().getSku(), allocated, requested);
            }
        }
        return fullyAllocated;
    }

    /** Ensures the picking task state machine allows this transition. */
    private void validateTransition(PickingTaskStatus current, PickingTaskStatus next) {
        if (current == null) return; // brand-new task — any initial status is fine
        boolean valid = switch (current) {
            case CREATED     -> next == PickingTaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == PickingTaskStatus.COMPLETED;
            case COMPLETED   -> false; // terminal state — no further transitions allowed
        };
        if (!valid) throw new IllegalStateException("Invalid transition: " + current + " → " + next);
    }

    /** Null-safe int getter — avoids scattered null checks everywhere. */
    private static int safe(Integer v) { return v == null ? 0 : v; }
}
