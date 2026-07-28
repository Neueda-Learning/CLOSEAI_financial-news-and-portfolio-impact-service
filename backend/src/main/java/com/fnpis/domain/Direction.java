package com.fnpis.domain;

/**
 * Expected direction of a news item's effect on a holding (requirement E2).
 *
 * <p>Derived from the sentiment of the article, not from the price. What the
 * price did is reported separately by {@link Alignment} - conflating the two is
 * exactly the mistake requirements 5.3 warns about.
 *
 * <p>Persist with {@code @Enumerated(EnumType.STRING)}.
 */
public enum Direction {

    /** Expected to help the position. */
    POSITIVE,

    /** Expected to hurt the position. */
    NEGATIVE,

    /** No directional expectation. */
    NEUTRAL
}
