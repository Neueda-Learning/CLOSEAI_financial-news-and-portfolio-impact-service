package com.fnpis.api.internal.dto;

import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * One assessed impact: what a story did to one holding on one session (E1-E4).
 *
 * <p>The element type of both the list endpoint and the linked view's
 * {@code impacts} array (API contract 4.2), so the two never drift apart.
 *
 * <p><b>The two questions stay separate.</b> {@code direction} is what the news
 * implied for the position, derived from sentiment; {@code alignment} is whether
 * the price agreed. They are never blended into one score, because disagreement
 * is the informative case (CLAUDE.md domain notes).
 *
 * @param holdingWeight the position's share of portfolio value at assessment
 *                      time, 0-1. Never null: a position that could not be
 *                      valued produces no row at all (EC-13) rather than a
 *                      zero-weight one that would read as "no impact"
 * @param priceChangePct the session's move as a percentage, null when neither a
 *                      close pair nor a quote was available (EC-18). A percentage
 *                      rather than a ratio because it is display-facing
 * @param expectedImpact weight times sentiment score - what the news implied
 * @param observedContribution weight times the actual return - what happened.
 *                      Null whenever {@code priceChangePct} is
 * @param valueImpact   the money figure, serialized as a string so JavaScript's
 *                      double cannot round it (CLAUDE.md). Null when the return
 *                      is unknown
 * @param alignment     INCONCLUSIVE covers both a sub-epsilon move and missing
 *                      price data - a real answer, not a failure to report
 */
@Schema(description = "One story's assessed effect on one holding")
public record ImpactRow(
        String symbol,
        String companyName,
        BigDecimal holdingWeight,
        Double priceChangePct,
        BigDecimal expectedImpact,
        BigDecimal observedContribution,
        BigDecimal valueImpact,
        Direction direction,
        Alignment alignment) {
}
