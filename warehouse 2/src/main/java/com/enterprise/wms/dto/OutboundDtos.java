package com.enterprise.wms.dto;

/**
 * DTOs for the outbound / picking workflow.
 */
public class OutboundDtos {

    /**
     * Request to create a picking task for an order.
     *
     * @param orderId        sales order to pick
     * @param strategy       picking strategy: SINGLE, BATCH, or WAVE
     * @param workerUsername worker assigned to the task
     */
    public record CreatePickingTaskRequest(Long orderId, String strategy, String workerUsername) {}

    /**
     * Request to update a picking task’s status and progress.
     *
     * @param status      new status: CREATED, IN_PROGRESS, or COMPLETED
     * @param progressPct completion percentage (0–100)
     */
    public record UpdatePickingStatusRequest(String status, Integer progressPct) {}
}
