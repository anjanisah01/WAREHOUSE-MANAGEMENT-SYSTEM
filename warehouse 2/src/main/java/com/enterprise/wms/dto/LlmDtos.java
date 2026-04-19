package com.enterprise.wms.dto;

/**
 * DTOs for the natural-language / AI command interface.
 */
public class LlmDtos {

    /** Free-text query sent by the user (e.g. "show low stock in WH-001"). */
    public record NaturalLanguageQuery(String text) {}

    /** Parsed action command returned by OpenAI or the fallback rule engine. */
    public record ActionCommand(String action, Integer days, String sku, String warehouseCode) {}

    /** Response when resolving/dismissing an alert. */
    public record AlertResolveResponse(Long id, Boolean resolved, String message) {}
}
