package com.acme.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructuredLogger {

    private static final Logger log = LoggerFactory.getLogger(StructuredLogger.class);
    private final String contextName;

    public StructuredLogger(String contextName) {
        this.contextName = contextName;
    }

    public String formatLog(String action, String entityId, Object detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(contextName).append("] ");
        sb.append("action=").append(action);
        sb.append(" entityId=").append(entityId);
        if (detail != null) {
            sb.append(" detail=").append(detail);
        }
        String logMessage = sb.toString();
        log.info(logMessage);
        return logMessage;
    }
}
