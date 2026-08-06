package com.acme.common.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void generatesPrefixedUniqueIds() {
        String first = Ids.newId("ord");
        String second = Ids.newId("ord");

        assertThat(first).startsWith("ord_").hasSize(16);
        assertThat(first).isNotEqualTo(second);
        assertThat(Ids.hasPrefix(first, "ord")).isTrue();
    }

    @Test
    void rejectsBlankPrefix() {
        assertThatThrownBy(() -> Ids.newId("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
