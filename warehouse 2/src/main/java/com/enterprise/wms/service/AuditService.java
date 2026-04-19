package com.enterprise.wms.service;

import com.enterprise.wms.domain.entity.AuditLog;
import com.enterprise.wms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/** Simple wrapper around the audit log repository; provides paginated access to audit entries. */
@Service
public class AuditService {

    private final AuditLogRepository auditRepo;

    public AuditService(AuditLogRepository auditRepo) { this.auditRepo = auditRepo; }

    /** Returns the first 50 audit log entries (shortcut). */
    public List<AuditLog> allLogs() {
        return allLogs(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id")))).getContent();
    }

    /** Returns a paginated view of audit log entries. Falls back to default page if null/unpaged. */
    public Page<AuditLog> allLogs(Pageable pageable) {
        Pageable safe = (pageable == null || pageable.isUnpaged())
                ? PageRequest.of(0, 50, Sort.by(Sort.Order.desc("id")))
                : pageable;
        return auditRepo.findAll(safe);
    }
}
