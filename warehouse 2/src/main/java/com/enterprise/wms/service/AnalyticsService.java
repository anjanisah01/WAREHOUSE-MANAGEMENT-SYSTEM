package com.enterprise.wms.service;

import com.enterprise.wms.domain.WmsEnums.AlertType;
import com.enterprise.wms.domain.entity.Alert;
import com.enterprise.wms.domain.entity.Inventory;
import com.enterprise.wms.domain.entity.LotBatch;
import com.enterprise.wms.repository.AlertRepository;
import com.enterprise.wms.repository.InventoryRepository;
import com.enterprise.wms.repository.LotBatchRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Rule engine that scans inventory for low-stock and near-expiry conditions,
 * creating alerts when thresholds are breached. Runs on demand and every 30 minutes via cron.
 */
@Service
public class AnalyticsService {

    private final InventoryRepository inventoryRepo;
    private final LotBatchRepository lotRepo;
    private final AlertRepository alertRepo;

    public AnalyticsService(InventoryRepository inventoryRepo,
                            LotBatchRepository lotRepo,
                            AlertRepository alertRepo) {
        this.inventoryRepo = inventoryRepo;
        this.lotRepo = lotRepo;
        this.alertRepo = alertRepo;
    }

    /** Runs all alert rules and returns newly created alerts. */
    @Transactional
    public List<Alert> runRuleEngine() {
        List<Alert> created = new ArrayList<>();

        // Load existing unresolved alerts to avoid duplicates
        List<Alert> lowAlerts    = new ArrayList<>(alertRepo.findByAlertTypeAndResolvedFalse(AlertType.LOW_STOCK));
        List<Alert> expiryAlerts = new ArrayList<>(alertRepo.findByAlertTypeAndResolvedFalse(AlertType.EXPIRY));

        // Rule 1: LOW_STOCK — quantity at or below reorder level
        for (Inventory inv : inventoryRepo.findAll()) {
            if (inv.getProduct() == null) continue;
            int qty     = inv.getQuantity() == null ? 0 : inv.getQuantity();
            int reorder = inv.getProduct().getReorderLevel() == null ? 0 : inv.getProduct().getReorderLevel();
            if (qty <= reorder) {
                String msg = "Low stock: " + inv.getProduct().getSku();
                addIfAbsent(lowAlerts, AlertType.LOW_STOCK, msg, msg, "HIGH").ifPresent(created::add);
            }
        }

        // Rule 2: EXPIRY — lots expiring within the next 7 days
        for (LotBatch lot : lotRepo.findByExpiryDateLessThanEqual(LocalDate.now().plusDays(7))) {
            String prefix = "Expiry soon for lot " + lot.getLotNo();
            String detail = prefix + (lot.getProduct() != null ? " / SKU " + lot.getProduct().getSku() : "");
            addIfAbsent(expiryAlerts, AlertType.EXPIRY, prefix, detail, "MEDIUM").ifPresent(created::add);
        }

        return created;
    }

    /** Returns all unresolved alerts, newest first. */
    public List<Alert> activeAlerts() {
        return alertRepo.findByResolvedFalseOrderByCreatedAtDesc();
    }

    /** Marks a single alert as resolved. */
    @Transactional
    public Alert resolveAlert(Long alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setResolved(true);
        return alertRepo.save(alert);
    }

    /** Scheduled background task — runs the rule engine every 30 minutes. */
    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void scheduledRuleCheck() { runRuleEngine(); }

    // ── Helper ──

    /** Creates and saves an alert only if no existing unresolved alert starts with the same prefix. */
    private Optional<Alert> addIfAbsent(List<Alert> existing, AlertType type,
                                        String prefix, String message, String severity) {
        boolean alreadyExists = existing.stream()
                .map(Alert::getMessage).filter(Objects::nonNull)
                .anyMatch(msg -> msg.startsWith(prefix));
        if (alreadyExists) return Optional.empty();

        Alert a = new Alert();
        a.setAlertType(type);
        a.setMessage(message);
        a.setSeverity(severity);
        a.setResolved(false);
        a.setCreatedAt(LocalDateTime.now());
        Alert saved = alertRepo.save(a);
        existing.add(saved); // track within this run to prevent duplicates
        return Optional.of(saved);
    }
}
