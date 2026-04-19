package com.enterprise.wms.service;

import com.enterprise.wms.dto.LlmDtos.ActionCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Parses natural-language warehouse queries into structured ActionCommand objects.
 * Uses the OpenAI Chat API when an API key is configured; otherwise falls back to
 * a simple keyword-based rule engine.
 */
@Service
public class OpenAiActionService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${wms.openai.api-key}") private String apiKey; // OpenAI API key
    @Value("${wms.openai.model}")   private String model;  // model name (e.g. gpt-4o-mini)

    /** Converts a free-text query into an ActionCommand. */
    public ActionCommand parseAction(String input) {
        // If no API key, skip the network call entirely
        if (apiKey == null || apiKey.isBlank()) return fallback(input);

        try {
            // Build the OpenAI chat-completion request asking for JSON output
            String prompt = """
                    Convert warehouse query to JSON with fields:
                    action, days, sku, warehouseCode.
                    If missing values, keep null.
                    Query: %s
                    """.formatted(input);

            String body = mapper.writeValueAsString(
                    mapper.createObjectNode()
                          .put("model", model)
                          .set("messages", mapper.createArrayNode()
                              .add(mapper.createObjectNode().put("role", "system").put("content", "You are a WMS intent parser. Return JSON only."))
                              .add(mapper.createObjectNode().put("role", "user").put("content", prompt))));

            Request req = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(RequestBody.create(body, MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response res = httpClient.newCall(req).execute()) {
                if (!res.isSuccessful() || res.body() == null) return fallback(input);

                // Extract the assistant's message content
                JsonNode root = mapper.readTree(res.body().string());
                String content = root.path("choices").get(0).path("message").path("content").asText("{}");
                JsonNode json = mapper.readTree(content);

                return new ActionCommand(
                        json.path("action").asText("UNKNOWN"),
                        json.path("days").isNumber()  ? json.path("days").asInt()       : null,
                        json.path("sku").isTextual()   ? json.path("sku").asText()       : null,
                        json.path("warehouseCode").isTextual() ? json.path("warehouseCode").asText() : null);
            }
        } catch (IOException e) { return fallback(input); }
    }

    /** Keyword-based fallback when OpenAI is unavailable. */
    private ActionCommand fallback(String input) {
        String t = input.toLowerCase();

        // Expiry-related queries
        if (t.contains("expiring") || t.contains("expiry") || t.contains("expire") || t.contains("best before")) {
            int days = 30;
            if (t.contains("7"))                                    days = 7;
            else if (t.contains("14") || t.contains("two week"))    days = 14;
            else if (t.contains("60") || t.contains("two month"))   days = 60;
            else if (t.contains("90"))                              days = 90;
            return new ActionCommand("GET_EXPIRING_PRODUCTS", days, null, null);
        }
        // Low stock / reorder queries
        if (t.contains("low stock") || t.contains("reorder") || t.contains("below") ||
                t.contains("running low") || t.contains("out of stock"))
            return new ActionCommand("GET_LOW_STOCK", null, null, null);
        // Warehouse status
        if (t.contains("warehouse") && (t.contains("status") || t.contains("overview") ||
                t.contains("summary") || t.contains("hub") || t.contains("depot")))
            return new ActionCommand("GET_WAREHOUSE_STATUS", null, null, null);
        // Movement / activity history
        if (t.contains("movement") || t.contains("activity") || t.contains("recent") ||
                t.contains("transfer") || t.contains("history") || t.contains("received") || t.contains("shipped"))
            return new ActionCommand("GET_MOVEMENTS", null, null, null);
        // Alerts / warnings
        if (t.contains("alert") || t.contains("warning") || t.contains("critical") ||
                t.contains("issue") || t.contains("problem"))
            return new ActionCommand("GET_ALERTS", null, null, null);
        // Dead stock / velocity classification
        if (t.contains("dead stock") || t.contains("slow") || t.contains("fast mov") ||
                t.contains("velocity") || t.contains("no movement"))
            return new ActionCommand("GET_DEAD_STOCK", null, null, null);
        // Picking tasks
        if (t.contains("pick") || t.contains("task") || t.contains("worker") || t.contains("wave"))
            return new ActionCommand("GET_PICKING_TASKS", null, null, null);
        // Orders / fulfilment
        if (t.contains("order") || t.contains("fulfil") || t.contains("dispatch") || t.contains("outbound"))
            return new ActionCommand("GET_ORDERS", null, null, null);
        // General inventory / stock summary
        if (t.contains("inventory") || t.contains("stock") || t.contains("sku") ||
                t.contains("total") || t.contains("summary") || t.contains("how many") || t.contains("how much"))
            return new ActionCommand("GET_INVENTORY_SUMMARY", null, null, null);

        return new ActionCommand("UNKNOWN", null, null, null); // unrecognised query
    }
}
