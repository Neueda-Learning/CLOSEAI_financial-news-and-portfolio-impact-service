package com.fnpis.domain;

/**
 * Sentiment direction for a headline (requirements 5.2).
 *
 * <p>Persist with {@code @Enumerated(EnumType.STRING)}. Storing the ordinal
 * means inserting a value into the middle of this enum silently corrupts every
 * historical row (architecture 6.2).
 *
 * <p>{@code NEUTRAL} is also the fallback: when the engine cannot decide it
 * must say NEUTRAL rather than return nothing. Null is not a valid outcome.
 */
public enum SentimentLabel {

    POSITIVE,
    NEGATIVE,
    NEUTRAL;

    /**
     * Whether {@code score} is consistent with this label.
     *
     * <p>A POSITIVE label with a negative score is self-contradictory output
     * and must be rejected rather than stored - one of the four validation
     * rules on LLM responses (architecture decision 5).
     *
     * @param score strength in [-1, 1]
     */
    public boolean isConsistentWith(java.math.BigDecimal score) {
        if (score == null) {
            return false;
        }
        int sign = score.signum();
        return switch (this) {
            case POSITIVE -> sign > 0;
            case NEGATIVE -> sign < 0;
            case NEUTRAL -> true;
        };
    }
}
