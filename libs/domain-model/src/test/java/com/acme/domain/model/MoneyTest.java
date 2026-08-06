package com.acme.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void testMoneyCreationAndFormatting() {
        Money m1 = Money.of("10.50", "USD");
        assertEquals("10.50 USD", m1.toString());
        assertTrue(m1.isPositive());
    }

    @Test
    void testMoneyArithmetic() {
        Money m1 = Money.of("10.50", "USD");
        Money m2 = Money.of("5.25", "USD");
        assertEquals(Money.of("15.75", "USD"), m1.plus(m2));
        assertEquals(Money.of("21.00", "USD"), m1.times(2));
    }

    @Test
    void testCurrencyMismatch() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");
        assertThrows(IllegalArgumentException.class, () -> usd.plus(eur));
    }
}
