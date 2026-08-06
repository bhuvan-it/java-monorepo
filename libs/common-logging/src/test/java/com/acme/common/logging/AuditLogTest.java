package com.acme.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AuditLogTest {

    private static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void stampsEventsWithComponentAndClock() {
        AuditLog log = new AuditLog("order-service", Clock.fixed(FIXED, ZoneOffset.UTC));

        AuditEvent event = log.record("ORDER_PLACED", "ord_123");

        assertThat(event.component()).isEqualTo("order-service");
        assertThat(event.action()).isEqualTo("ORDER_PLACED");
        assertThat(event.subject()).isEqualTo("ord_123");
        assertThat(event.occurredAt()).isEqualTo(FIXED);
        assertThat(event.eventId()).startsWith("evt_");
    }

    @Test
    void eachEventGetsItsOwnId() {
        AuditLog log = new AuditLog("inventory-service");

        assertThat(log.record("A", "s").eventId())
                .isNotEqualTo(log.record("A", "s").eventId());
    }
}
