package com.enterprise.wms.repository;

import com.enterprise.wms.domain.WmsEnums.AlertType;
import com.enterprise.wms.domain.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link Alert} entities.
 * Provides CRUD plus custom queries for unresolved alerts.
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /** Find unresolved alerts of a specific type (e.g. LOW_STOCK). */
    List<Alert> findByAlertTypeAndResolvedFalse(AlertType type);

    /** All unresolved alerts, newest first. */
    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();
}
