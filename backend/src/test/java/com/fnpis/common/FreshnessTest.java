package com.fnpis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FreshnessTest {

    private static final Instant NOW = Instant.parse("2026-07-27T15:00:00Z");
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    @DisplayName("data captured inside the budget is not stale")
    void withinBudgetIsFresh() {
        Freshness result = Freshness.of(NOW.minusSeconds(30), ONE_MINUTE, NOW);

        assertThat(result.stale()).isFalse();
        assertThat(result.asOf()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("data older than the budget is stale")
    void pastBudgetIsStale() {
        assertThat(Freshness.of(NOW.minusSeconds(90), ONE_MINUTE, NOW).stale()).isTrue();
    }

    @Test
    @DisplayName("exactly at the budget is still fresh, not stale")
    void boundaryIsFresh() {
        // Guards the comparison against flipping to >=, which would mark data
        // stale a tick early and make the demo look worse than it is.
        assertThat(Freshness.of(NOW.minusSeconds(60), ONE_MINUTE, NOW).stale()).isFalse();
    }

    @Test
    @DisplayName("never-fetched data is stale with a null timestamp")
    void nullCaptureIsStale() {
        Freshness result = Freshness.of(null, ONE_MINUTE, NOW);

        assertThat(result.stale()).isTrue();
        assertThat(result.asOf()).isNull();
    }
}
