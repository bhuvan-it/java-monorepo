package com.acme.common.logging;

import com.acme.common.core.Money;

public class StructuredLogger {

    private final String contextName;

    public StructuredLogger(String contextName) {
        this.contextName = contextName;
    }

    public String formatLog(String action, String entityId, Money amount) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(contextName).append("] ");
        sb.append("action=").append(action);
        sb.append(" entityId=").append(entityId);
        if (amount != null) {
            sb.append(" amount=").append(amount);
        }
        return sb.toString();
    }
}
