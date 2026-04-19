package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link AuditLog} entries (paginated access). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Return a page of audit log entries (overrides default for explicit pagination). */
    Page<AuditLog> findAll(Pageable pageable);
}
