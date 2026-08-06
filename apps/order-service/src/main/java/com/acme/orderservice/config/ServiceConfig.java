package com.acme.orderservice.config;

import com.acme.common.logging.AuditLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public AuditLog auditLog(@Value("${spring.application.name}") String appName) {
        return new AuditLog(appName);
    }
}
