package com.acme.common.logging;

import com.acme.common.core.Ids;
import java.time.Clock;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emits audit lines on a dedicated {@code audit} logger so they can be routed independently of
 * application logs.
 */
public final class AuditLog {

    private static final Logger LOG = LoggerFactory.getLogger("audit");

    private final String component;
    private final Clock clock;

    public AuditLog(String component) {
        this(component, Clock.systemUTC());
    }

    public AuditLog(String component, Clock clock) {
        this.component = Objects.requireNonNull(component, "component");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AuditEvent record(String action, String subject) {
        AuditEvent event = new AuditEvent(Ids.newId("evt"), component, action, subject, clock.instant());
        LOG.info(
                "audit component={} action={} subject={} eventId={} at={}",
                event.component(),
                event.action(),
                event.subject(),
                event.eventId(),
                event.occurredAt());
        return event;
    }
}
