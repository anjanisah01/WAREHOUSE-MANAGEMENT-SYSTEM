package com.enterprise.wms.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * HTTP request audit log entry, recorded by {@link com.enterprise.wms.config.AuditInterceptor}.
 * Captures every API call with user, method, path, status, and timestamp.
 */
@Entity
@Getter @Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;                     // Authenticated user (or "anonymous")
    private String method;                       // HTTP method: GET, POST, PUT, DELETE
    private String path;                         // Request URI path
    private Integer statusCode;                  // HTTP response status code
    private LocalDateTime eventTime;             // When the request was processed
}
