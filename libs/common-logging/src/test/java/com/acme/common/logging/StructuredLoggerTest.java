package com.acme.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.common.core.Money;
import org.junit.jupiter.api.Test;

class StructuredLoggerTest {

    @Test
    void shouldFormatStructuredLogWithMoney() {
        StructuredLogger logger = new StructuredLogger("ORDER");
        String formatted = logger.formatLog("CREATE", "ORD-123", Money.of(99.99, "USD"));

        assertThat(formatted).isEqualTo("[ORDER] action=CREATE entityId=ORD-123 amount=99.99 USD");
    }

    @Test
    void shouldFormatStructuredLogWithoutMoney() {
        StructuredLogger logger = new StructuredLogger("INVENTORY");
        String formatted = logger.formatLog("RESERVE", "ITEM-456", null);

        assertThat(formatted).isEqualTo("[INVENTORY] action=RESERVE entityId=ITEM-456");
    }
}
