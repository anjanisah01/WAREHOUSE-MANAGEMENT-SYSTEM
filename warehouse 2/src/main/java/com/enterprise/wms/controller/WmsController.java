package com.enterprise.wms.controller;

import com.enterprise.wms.domain.WmsEnums.PickingStrategy;
import com.enterprise.wms.domain.WmsEnums.PickingTaskStatus;
import com.enterprise.wms.domain.entity.Alert;
import com.enterprise.wms.domain.entity.AuditLog;
import com.enterprise.wms.domain.entity.Inventory;
import com.enterprise.wms.domain.entity.Location;
import com.enterprise.wms.domain.entity.MovementHistory;
import com.enterprise.wms.domain.entity.PickingTask;
import com.enterprise.wms.domain.entity.SalesOrder;
import com.enterprise.wms.dto.InboundDtos.GoodsReceiptRequest;
import com.enterprise.wms.dto.LlmDtos.ActionCommand;
import com.enterprise.wms.dto.LlmDtos.AlertResolveResponse;
import com.enterprise.wms.dto.LlmDtos.NaturalLanguageQuery;
import com.enterprise.wms.dto.OrderDtos.CreateOrderRequest;
import com.enterprise.wms.dto.OrderDtos.WavePlanRequest;
import com.enterprise.wms.dto.OutboundDtos.CreatePickingTaskRequest;
import com.enterprise.wms.dto.OutboundDtos.UpdatePickingStatusRequest;
import com.enterprise.wms.dto.TransferDtos.TransferRequest;
import com.enterprise.wms.service.AnalyticsService;
import com.enterprise.wms.service.AuditService;
import com.enterprise.wms.service.InboundService;
import com.enterprise.wms.service.InventoryService;
import com.enterprise.wms.service.OpenAiActionService;
import com.enterprise.wms.service.OutboundService;
import com.enterprise.wms.service.PutawayService;
import com.enterprise.wms.service.TransferService;
import com.enterprise.wms.service.VoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main WMS controller surface.
 * Exposes operational REST APIs and also includes the Thymeleaf UI entrypoints.
 */
@RestController
@RequestMapping("/api/wms")
public class WmsController {
    private final InboundService inboundService;
    private final PutawayService putawayService;
    private final OutboundService outboundService;
    private final InventoryService inventoryService;
    private final AnalyticsService analyticsService;
    private final AuditService auditService;
    private final OpenAiActionService openAiActionService;
    private final VoiceService voiceService;
    private final TransferService transferService;

    public WmsController(InboundService inboundService,
                         PutawayService putawayService,
                         OutboundService outboundService,
                         InventoryService inventoryService,
                         AnalyticsService analyticsService,
                         AuditService auditService,
                         OpenAiActionService openAiActionService,
                         VoiceService voiceService,
                         TransferService transferService) {
        this.inboundService = inboundService;
        this.putawayService = putawayService;
        this.outboundService = outboundService;
        this.inventoryService = inventoryService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
        this.openAiActionService = openAiActionService;
        this.voiceService = voiceService;
        this.transferService = transferService;
    }

    @PostMapping("/inbound/grn")
    public Inventory receive(@RequestBody GoodsReceiptRequest request) {
        return inboundService.receiveGoods(request);
    }

    @GetMapping("/putaway/suggest/{warehouseCode}")
    public Location suggest(@PathVariable String warehouseCode) {
        return putawayService.suggestStorageLocation(warehouseCode);
    }

    @GetMapping("/putaway/suggest/{warehouseCode}/{productId}")
    public Location suggestForProduct(@PathVariable String warehouseCode, @PathVariable Long productId) {
        return putawayService.suggestStorageLocation(warehouseCode, productId);
    }

    @PostMapping("/outbound/order")
    public Map<String, Object> createOrder(@RequestBody CreateOrderRequest request) {
        return Map.of("order", outboundService.createOrder(request));
    }

    @GetMapping("/outbound/orders")
    public List<SalesOrder> listOrders() {
        return outboundService.listOrders();
    }

    @PostMapping("/outbound/picking-task")
    public PickingTask createPickingTask(@RequestBody CreatePickingTaskRequest request) {
        return outboundService.createPickingTask(
                request.orderId(),
                parsePickingStrategy(request.strategy()),
                request.workerUsername()
        );
    }

    @GetMapping("/outbound/picking-tasks")
    public List<PickingTask> activePickingTasks() {
        return outboundService.activePickingTasks();
    }

    @PutMapping("/outbound/picking-task/{id}/status")
    public PickingTask updatePickingStatus(@PathVariable Long id, @RequestBody UpdatePickingStatusRequest request) {
        return outboundService.updatePickingStatus(
                id,
                parsePickingStatus(request.status()),
                request.progressPct()
        );
    }

    @PostMapping("/outbound/wave-plan")
    public List<PickingTask> createWave(@RequestBody WavePlanRequest request) {
        return outboundService.createWavePickingTasks(
                request.warehouseCode(), request.waveNo(), request.orderIds(), request.workerUsername());
    }

    @PostMapping("/outbound/ship/{orderId}")
    public Map<String, Object> ship(@PathVariable Long orderId) {
        return Map.of("order", outboundService.markShipped(orderId));
    }

    @GetMapping("/inventory/stock")
    public Page<Inventory> stock(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "50") int size,
                                 @RequestParam(defaultValue = "id,desc") String sort) {
        return inventoryService.realTimeStock(buildPageable(page, size, sort));
    }

    @GetMapping("/inventory/movements")
    public Page<MovementHistory> movements(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size,
                                           @RequestParam(defaultValue = "id,desc") String sort) {
        return inventoryService.movementHistory(buildPageable(page, size, sort));
    }

    @PostMapping("/inventory/cycle-count/{inventoryId}")
    public Inventory cycleCount(@PathVariable Long inventoryId, @RequestBody Map<String, String> body) {
        return inventoryService.cycleCountAdjust(
                inventoryId,
                Integer.parseInt(body.get("actualQty")),
                body.getOrDefault("performedBy", "system"));
    }

    @PostMapping("/inventory/transfer")
    public Inventory transfer(@RequestBody TransferRequest request) {
        return transferService.transfer(
                request.fromWarehouseCode(),
                request.toWarehouseCode(),
                request.sku(),
                request.quantity(),
                request.performedBy());
    }

    @PostMapping("/analytics/run-rules")
    public List<Alert> runRules() {
        return analyticsService.runRuleEngine();
    }

    @GetMapping("/analytics/replenishment")
    public List<Alert> replenishment() {
        return inventoryService.replenishmentAlerts();
    }

    @GetMapping("/analytics/movement-classification")
    public Map<String, List<String>> movementClassification() {
        return inventoryService.fastSlowDeadStock();
    }

    @GetMapping("/alerts")
    public List<Alert> alerts() {
        return analyticsService.activeAlerts();
    }

    @PostMapping("/alerts/{id}/resolve")
    public AlertResolveResponse resolveAlert(@PathVariable Long id) {
        Alert alert = analyticsService.resolveAlert(id);
        return new AlertResolveResponse(alert.getId(), alert.getResolved(), alert.getMessage());
    }

    @GetMapping("/audit/logs")
    public Page<AuditLog> auditLogs(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "50") int size,
                                    @RequestParam(defaultValue = "id,desc") String sort) {
        return auditService.allLogs(buildPageable(page, size, sort));
    }

    @PostMapping("/llm/parse-action")
    public ActionCommand parse(@RequestBody NaturalLanguageQuery query) {
        return openAiActionService.parseAction(query.text());
    }

    @PostMapping("/voice/query")
    public ActionCommand voiceQuery(@RequestBody byte[] audio) {
        String text = voiceService.speechToText(audio);
        return openAiActionService.parseAction(text);
    }

    @PostMapping("/voice/respond")
    public ResponseEntity<byte[]> tts(@RequestBody Map<String, String> payload) {
        String format = payload.get("format");
        byte[] data = voiceService.textToSpeech(payload.getOrDefault("text", "No response"), format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, voiceService.ttsContentType(format))
                .body(data);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        // Applies safe defaults and clamps page size to protect endpoints from huge requests.
        String[] parts = sort == null ? new String[0] : sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0].trim() : "id";
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromOptionalString(parts[1].trim()).orElse(Sort.Direction.DESC)
                : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(direction, property));
    }

    private PickingStrategy parsePickingStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("Picking strategy is required");
        }
        return PickingStrategy.valueOf(strategy.trim().toUpperCase());
    }

    private PickingTaskStatus parsePickingStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Picking status is required");
        }
        return PickingTaskStatus.valueOf(status.trim().toUpperCase());
    }
}

/**
 * Server-rendered UI controller used by the dashboard and login pages.
 */
@Controller
class WmsUiController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final InventoryService inventoryService;

    WmsUiController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("title", "Enterprise WMS Login");
        model.addAttribute("appName", "Enterprise WMS");
        model.addAttribute("demoUsername", "admin");
        model.addAttribute("demoPassword", "admin123");
        return "login";
    }

    @GetMapping("/app")
    public String home(Model model) {
        List<Inventory> stock = inventoryService.realTimeStock();
        List<MovementHistory> movements = inventoryService.movementHistory();

        model.addAttribute("title", "Enterprise WMS");
        model.addAttribute("modules", List.of("Dashboard", "Inventory", "Outbound", "AI Insights", "Voice", "Audit Logs", "Reports", "Settings"));
        model.addAttribute("warehouses", stock.stream()
                .map(item -> item.getWarehouse() != null ? safe(item.getWarehouse().getCode(), "UNASSIGNED") : "UNASSIGNED")
                .distinct()
                .sorted()
                .toList());
        model.addAttribute("inventoryData", stock.stream().map(this::toInventoryRow).toList());
        model.addAttribute("movementData", movements.stream()
            .sorted(Comparator.comparing(MovementHistory::getEventTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(20)
                .map(this::toMovementRow)
                .toList());
        return "dashboard";
    }

    private Map<String, Object> toInventoryRow(Inventory item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getProduct() != null ? safe(item.getProduct().getBarcode(), "N/A") : "N/A");
        row.put("sku", item.getProduct() != null ? safe(item.getProduct().getSku(), "N/A") : "N/A");
        row.put("name", item.getProduct() != null ? safe(item.getProduct().getName(), "Unnamed Item") : "Unnamed Item");
        row.put("qty", item.getQuantity() != null ? item.getQuantity() : 0);
        row.put("reservedQty", item.getReservedQty() != null ? item.getReservedQty() : 0);
        row.put("min", item.getProduct() != null && item.getProduct().getReorderLevel() != null ? item.getProduct().getReorderLevel() : 10);
        row.put("warehouse", item.getWarehouse() != null ? safe(item.getWarehouse().getCode(), "UNASSIGNED") : "UNASSIGNED");
        row.put("zone", item.getLocation() != null ? safe(item.getLocation().getZone(), "NA") : "NA");
        row.put("location", formatLocation(item.getLocation()));
        row.put("lotNo", item.getLotBatch() != null ? safe(item.getLotBatch().getLotNo(), "NA") : "NA");
        row.put("expiry", item.getLotBatch() != null && item.getLotBatch().getExpiryDate() != null
                ? item.getLotBatch().getExpiryDate().toString()
                : LocalDate.now().plusDays(90).toString());
        row.put("category", item.getProduct() != null && item.getProduct().getVelocityClass() != null
                ? item.getProduct().getVelocityClass().name()
                : "STANDARD");
        return row;
    }

    private Map<String, Object> toMovementRow(MovementHistory movement) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("time", movement.getEventTime() != null ? movement.getEventTime().format(TIME_FORMATTER) : "--:--");
        row.put("type", movement.getMovementType() != null ? movement.getMovementType().name() : "INFO");
        row.put("product", movement.getProduct() != null ? safe(movement.getProduct().getName(), "Unknown Item") : "Unknown Item");
        row.put("qty", movement.getQuantity() != null ? movement.getQuantity() : 0);
        row.put("user", safe(movement.getPerformedBy(), "system"));
        return row;
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return "NA-R0-S0-B0";
        }
        return "%s-%s-%s-%s".formatted(
                safe(location.getZone(), "NA"),
                safe(location.getRack(), "R0"),
                safe(location.getShelf(), "S0"),
                safe(location.getBin(), "B0")
        );
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
