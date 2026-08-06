package com.acme.common.logging;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StructuredLoggerTest {

    @Test
    void testFormatLog() {
        StructuredLogger logger = new StructuredLogger("TEST-SERVICE");
        String formatted = logger.formatLog("CREATE", "ID-123", "10.00 USD");
        assertTrue(formatted.contains("[TEST-SERVICE]"));
        assertTrue(formatted.contains("action=CREATE"));
        assertTrue(formatted.contains("entityId=ID-123"));
        assertTrue(formatted.contains("detail=10.00 USD"));
    }
}
