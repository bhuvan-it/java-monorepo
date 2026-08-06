package com.acme.common.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void okCarriesItsValue() {
        Result<String> result = Result.ok("hello");

        assertThat(result.isOk()).isTrue();
        assertThat(result.orElseThrow()).isEqualTo("hello");
    }

    @Test
    void errShortCircuitsMapping() {
        Result<String> result = Result.<String>err("boom").map(String::toUpperCase);

        assertThat(result.isOk()).isFalse();
        assertThat(result.orElse("fallback")).isEqualTo("fallback");
        assertThatThrownBy(result::orElseThrow)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("boom");
    }

    @Test
    void flatMapChainsSuccessfulSteps() {
        Result<Integer> result = Result.ok("42").flatMap(s -> Result.ok(Integer.parseInt(s)));

        assertThat(result.orElseThrow()).isEqualTo(42);
    }
}
