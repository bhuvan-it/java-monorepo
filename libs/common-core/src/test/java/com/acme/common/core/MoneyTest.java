package com.acme.common.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithScaledAmount() {
        Money m = Money.of(10.5, "USD");
        assertThat(m.getAmount()).isEqualTo("10.50");
        assertThat(m.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldAddSameCurrency() {
        Money m1 = Money.of(10.0, "USD");
        Money m2 = Money.of(15.5, "USD");
        Money result = m1.add(m2);

        assertThat(result).isEqualTo(Money.of(25.50, "USD"));
    }

    @Test
    void shouldFailToAddDifferentCurrencies() {
        Money m1 = Money.of(10.0, "USD");
        Money m2 = Money.of(15.5, "EUR");

        assertThatThrownBy(() -> m1.add(m2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot operate on different currencies");
    }

    @Test
    void shouldDisallowNegativeAmount() {
        assertThatThrownBy(() -> Money.of(-5.0, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot be negative");
    }
}
