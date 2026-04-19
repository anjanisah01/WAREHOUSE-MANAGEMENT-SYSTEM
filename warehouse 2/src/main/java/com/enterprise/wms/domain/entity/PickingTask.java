package com.enterprise.wms.domain.entity;

import com.enterprise.wms.domain.WmsEnums.PickingStrategy;
import com.enterprise.wms.domain.WmsEnums.PickingTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A task assigned to a warehouse worker to pick items for an order.
 * Supports SINGLE, BATCH, and WAVE picking strategies.
 */
@Entity
@Getter @Setter
@Table(name = "picking_task")
public class PickingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)                 // The order this task fulfils
    @JoinColumn(name = "order_id")
    private SalesOrder orderRef;

    private String workerUsername;               // Worker assigned to this task
    private String waveNo;                       // Wave number (null for single picks)

    @Enumerated(EnumType.STRING)                 // SINGLE / BATCH / WAVE
    private PickingStrategy strategy;

    @Enumerated(EnumType.STRING)                 // CREATED / IN_PROGRESS / COMPLETED
    private PickingTaskStatus status;

    @Column(name = "progress_pct", nullable = false) // 0-100 completion percentage
    private Integer pickingProgressPct = 0;

    private LocalDateTime createdAt;             // When the task was created
}
