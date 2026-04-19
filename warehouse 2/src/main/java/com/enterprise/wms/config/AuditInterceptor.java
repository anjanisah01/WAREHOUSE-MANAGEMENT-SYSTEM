package com.enterprise.wms.config;

import com.enterprise.wms.domain.entity.AuditLog;
import com.enterprise.wms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * Spring MVC interceptor that logs every /api/** request into the audit_log table.
 * Captures: username, HTTP method, request path, response status, and timestamp.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditRepo;

    public AuditInterceptor(AuditLogRepository auditRepo) { this.auditRepo = auditRepo; }

    /** Called after the request has completed — persists an AuditLog entry. */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuditLog entry = new AuditLog();

        // Resolve the authenticated username (or "anonymous")
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        entry.setUsername(auth == null ? "anonymous" : auth.getName());

        entry.setMethod(request.getMethod());           // GET, POST, PUT, DELETE, etc.
        entry.setPath(request.getRequestURI());          // e.g. /api/wms/inventory
        entry.setStatusCode(response.getStatus());       // HTTP status code (200, 404, etc.)
        entry.setEventTime(LocalDateTime.now());         // when it happened
        auditRepo.save(entry);
    }
}
