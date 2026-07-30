package com.fnpis.domain;

/**
 * Whether the price move backed up what the news implied (requirements 5.3).
 *
 * <p>This is the project's most interesting output. The system answers two
 * separate questions - what the news <i>should</i> have done to the position,
 * and what the price <i>actually</i> did - and this enum reports whether the
 * two agree. Keeping them separate rather than blending them into one score is
 * deliberate: the disagreement is the informative case.
 *
 * <p>Persist with {@code @Enumerated(EnumType.STRING)}.
 */
public enum Alignment {

    /** Sentiment and price moved the same way. */
    CONFIRMED,

    /** Sentiment and price moved opposite ways - the honest failure case. */
    DIVERGENT,

    /**
     * Price barely moved, or a needed input was missing.
     *
     * <p>Covers two situations that must not be reported as a verdict: the move
     * was smaller than epsilon and is therefore noise, and the previous close
     * was unavailable so no return could be computed at all (EC-18).
     */
    INCONCLUSIVE
}
