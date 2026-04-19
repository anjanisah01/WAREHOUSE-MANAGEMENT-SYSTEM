package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.AlertType;
import com.enterprise.wms.domain.WmsEnums.MovementType;
import com.enterprise.wms.domain.entity.Alert;
import com.enterprise.wms.domain.entity.Inventory;
import com.enterprise.wms.domain.entity.MovementHistory;
import com.enterprise.wms.repository.AlertRepository;
import com.enterprise.wms.repository.InventoryRepository;
import com.enterprise.wms.repository.MovementHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides inventory queries, cycle-count adjustments,
 * replenishment alerts, and fast/slow/dead stock classification.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepo; // stock levels
    private final MovementHistoryRepository moveRepo; // movement logs
    private final AlertRepository alertRepo;          // alert storage

    public InventoryService(InventoryRepository inventoryRepo,
                            MovementHistoryRepository moveRepo,
                            AlertRepository alertRepo) {
        this.inventoryRepo = inventoryRepo;
        this.moveRepo = moveRepo;
        this.alertRepo = alertRepo;
    }

    // ── Real-time stock queries ──

    /** Returns the first 50 inventory rows (shortcut for the default page). */
    public List<Inventory> realTimeStock() {
        return realTimeStock(defaultPage()).getContent();
    }

    /** Returns a paginated view of all inventory. */
    public Page<Inventory> realTimeStock(Pageable pageable) {
        return inventoryRepo.findAll(safe(pageable));
    }

    // ── Movement history queries ──

    /** Returns the first 50 movement records (shortcut). */
    public List<MovementHistory> movementHistory() {
        return movementHistory(defaultPage()).getContent();
    }

    /** Returns a paginated view of all movement history. */
    public Page<MovementHistory> movementHistory(Pageable pageable) {
        return moveRepo.findAll(safe(pageable));
    }

    // ── Cycle count adjustment ──

    /** Updates an inventory row to the physically counted quantity, recording the delta as an ADJUSTMENT movement. */
    @Transactional
    public Inventory cycleCountAdjust(Long inventoryId, Integer actualQty, String performedBy) {
        Inventory inv = inventoryRepo.findById(inventoryId).orElseThrow();
        int before = inv.getQuantity() == null ? 0 : inv.getQuantity(); // old quantity
        int delta  = actualQty - before;                                 // difference (may be negative)

        inv.setQuantity(actualQty);     // set to the actual counted quantity
        inventoryRepo.save(inv);

        // Record the adjustment movement for audit
        MovementHistory adj = new MovementHistory();
        adj.setProduct(inv.getProduct());
        adj.setQuantity(Math.abs(delta));             // always store a positive quantity
        adj.setMovementType(MovementType.ADJUSTMENT);
        adj.setEventTime(LocalDateTime.now());
        adj.setPerformedBy(performedBy);
        adj.setReferenceNo("CYCLE-COUNT");
        moveRepo.save(adj);

        return inv;
    }

    // ── Replenishment alerts ──

    /** Scans all inventory; creates a REPLENISHMENT alert for any SKU at or below its reorder level. */
    @Transactional
    public List<Alert> replenishmentAlerts() {
        List<Alert> created = new ArrayList<>();
        // Load existing unresolved replenishment alerts to avoid duplicates
        List<Alert> existing = new ArrayList<>(alertRepo.findByAlertTypeAndResolvedFalse(AlertType.REPLENISHMENT));

        for (Inventory inv : inventoryRepo.findAll()) {
            if (inv.getProduct() == null) continue; // skip orphan rows
            int qty     = inv.getQuantity() == null ? 0 : inv.getQuantity();
            int reorder = inv.getProduct().getReorderLevel() == null ? 0 : inv.getProduct().getReorderLevel();

            if (qty <= reorder) { // stock is at or below reorder level
                String msg = "Replenish SKU " + inv.getProduct().getSku();
                createAlertIfAbsent(existing, AlertType.REPLENISHMENT, msg, msg, "HIGH")
                        .ifPresent(created::add);
            }
        }
        return created;
    }

    // ── Fast / Slow / Dead stock classification ──

    /** Classifies SKUs by movement frequency over the last 30 days. */
    public Map<String, List<String>> fastSlowDeadStock() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Count movements per SKU in the last 30 days
        Map<String, Long> counts = moveRepo.findByEventTimeAfter(since).stream()
                .filter(m -> m.getProduct() != null && m.getProduct().getSku() != null)
                .collect(Collectors.groupingBy(m -> m.getProduct().getSku(), Collectors.counting()));

        // Sort SKUs by movement count descending
        List<String> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();

        List<String> fast = sorted.stream().limit(5).toList();                        // top 5 movers
        List<String> slow = sorted.stream().skip(Math.max(0, sorted.size() - 5)).toList(); // bottom 5

        // Dead stock = SKUs with inventory but zero movements in 30 days
        List<String> dead = inventoryRepo.findAll().stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getSku() != null)
                .map(i -> i.getProduct().getSku())
                .filter(sku -> !counts.containsKey(sku))
                .distinct()
                .toList();

        return Map.of("fastMoving", fast, "slowMoving", slow, "deadStock", dead);
    }

    // ── Helpers ──

    /** Falls back to a default page if the caller passes null or unpaged. */
    private Pageable safe(Pageable p) {
        return (p == null || p.isUnpaged()) ? defaultPage() : p;
    }

    /** Default pagination: first 50 rows ordered by id descending. */
    private static Pageable defaultPage() {
        return PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id")));
    }

    /** Creates and persists an alert only if no existing unresolved alert starts with the same message prefix. */
    private Optional<Alert> createAlertIfAbsent(List<Alert> existing, AlertType type,
                                                String prefix, String message, String severity) {
        boolean alreadyExists = existing.stream()
                .map(Alert::getMessage)
                .filter(Objects::nonNull)
                .anyMatch(msg -> msg.startsWith(prefix));
        if (alreadyExists) return Optional.empty();

        Alert alert = new Alert();
        alert.setAlertType(type);
        alert.setMessage(message);
        alert.setSeverity(severity);
        alert.setResolved(false);
        alert.setCreatedAt(LocalDateTime.now());
        Alert saved = alertRepo.save(alert);
        existing.add(saved); // track to avoid duplicates within the same scan
        return Optional.of(saved);
    }
}
