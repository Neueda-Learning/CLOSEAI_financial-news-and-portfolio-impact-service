package com.fnpis.service;

import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import java.math.BigDecimal;

/**
 * What one story did to one holding - the output of {@link ImpactEngine}.
 *
 * <p>Ratios here are ratios, not percentages. {@code observedContribution}
 * 0.012580 is the +1.258% the API renders; the x100 happens once, in the DTO
 * layer. Doing it here as well yields 125.8%.
 *
 * <p>The three numeric fields that depend on r are null together, never
 * partially populated: with no return there is no observed side of the story to
 * report, and a zero would read as "the price did not move" rather than "we do
 * not know" (EC-18).
 *
 * @param expectedImpact        s * c * w, signed. What the sentiment predicted.
 *                              Always present - it needs no price data
 * @param observedContribution  w * r, how many points this holding moved the
 *                              portfolio. Null when r is null
 * @param valueImpact           holding value * r, the money figure on screen.
 *                              Null when r is null
 * @param direction             derived from sentiment alone (E2)
 * @param alignment             whether the price agreed with that direction (E4)
 */
public record ImpactOutput(
        BigDecimal expectedImpact,
        BigDecimal observedContribution,
        BigDecimal valueImpact,
        Direction direction,
        Alignment alignment) {
}