package com.fnpis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the label/score consistency rule (requirements 5.2).
 *
 * <p>This is one of the four checks applied to LLM output before it is trusted.
 * A model can return a well-formed response that is internally contradictory,
 * and storing that would put a green POSITIVE badge next to a negative score.
 */
class SentimentLabelTest {

    @Test
    @DisplayName("POSITIVE requires a score above zero")
    void positiveNeedsPositiveScore() {
        assertThat(SentimentLabel.POSITIVE.isConsistentWith(new BigDecimal("0.72"))).isTrue();
        assertThat(SentimentLabel.POSITIVE.isConsistentWith(new BigDecimal("-0.50"))).isFalse();
        assertThat(SentimentLabel.POSITIVE.isConsistentWith(BigDecimal.ZERO)).isFalse();
    }

    @Test
    @DisplayName("NEGATIVE requires a score below zero")
    void negativeNeedsNegativeScore() {
        assertThat(SentimentLabel.NEGATIVE.isConsistentWith(new BigDecimal("-0.31"))).isTrue();
        assertThat(SentimentLabel.NEGATIVE.isConsistentWith(new BigDecimal("0.31"))).isFalse();
        assertThat(SentimentLabel.NEGATIVE.isConsistentWith(BigDecimal.ZERO)).isFalse();
    }

    @Test
    @DisplayName("NEUTRAL accepts any score including the extremes")
    void neutralAcceptsAnything() {
        assertThat(SentimentLabel.NEUTRAL.isConsistentWith(BigDecimal.ZERO)).isTrue();
        assertThat(SentimentLabel.NEUTRAL.isConsistentWith(new BigDecimal("0.10"))).isTrue();
        assertThat(SentimentLabel.NEUTRAL.isConsistentWith(new BigDecimal("-0.10"))).isTrue();
    }

    @Test
    @DisplayName("a null score is never consistent")
    void nullScoreRejected() {
        // Null is not a permitted sentiment output - undecidable must come back
        // as NEUTRAL with a real score, not as an absent value.
        for (SentimentLabel label : SentimentLabel.values()) {
            assertThat(label.isConsistentWith(null)).isFalse();
        }
    }
}
