package com.acme.common.logging;

import java.time.Instant;
import java.util.Objects;

/** One immutable audit record. Returned to callers so the id can be echoed back to clients. */
public record AuditEvent(String eventId, String component, String action, String subject, Instant occurredAt) {

    public AuditEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
