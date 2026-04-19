package com.enterprise.wms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers MVC interceptors. Currently adds the AuditInterceptor for all /api/** paths. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;

    public WebMvcConfig(AuditInterceptor auditInterceptor) { this.auditInterceptor = auditInterceptor; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Every /api/** request will be logged in the audit_log table
        registry.addInterceptor(auditInterceptor).addPathPatterns("/api/**");
    }
}
