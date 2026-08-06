package com.acme.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void testMoneyCreationAndEquality() {
        Money m1 = Money.of("10.50", "USD");
        Money m2 = new Money(new BigDecimal("10.50"), "USD");

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.amount()).isEqualTo(new BigDecimal("10.50"));
        assertThat(m1.currency()).isEqualTo("USD");
    }

    @Test
    void testMoneyAddition() {
        Money m1 = Money.of("10.00", "USD");
        Money m2 = Money.of("5.50", "USD");
        Money sum = m1.plus(m2);

        assertThat(sum.amount()).isEqualTo(new BigDecimal("15.50"));
    }

    @Test
    void testMismatchedCurrencyAdditionThrows() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("5.50", "EUR");

        assertThatThrownBy(() -> usd.plus(eur))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
