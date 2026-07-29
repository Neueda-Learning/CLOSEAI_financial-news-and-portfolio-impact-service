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
 * <p><b>BigDecimal fields serialise as JSON strings, Double fields as numbers.</b>
 * Same split as {@link HoldingRow}, enforced globally by {@code JacksonConfig}.
 * Contract 1.2 puts money in strings and ratios, weights and percentages in
 * numbers, so only {@code valueImpact} is BigDecimal here. Typing a weight as
 * BigDecimal would emit {@code "0.303000"} with quotes, and the frontend's
 * {@code weight * 100} would then be string concatenation or NaN.
 *
 * @param holdingWeight the position's share of portfolio value at assessment
 *                      time, 0-1, as a display number. Never null: a position
 *                      that could not be valued produces no row at all (EC-13)
 *                      rather than a zero-weight one that would read as "no
 *                      impact"
 * @param priceChangePct the session's move as a percentage, null when neither a
 *                      close pair nor a quote was available (EC-18). A percentage
 *                      rather than a ratio because it is display-facing
 * @param expectedImpact weight times sentiment score - what the news implied.
 *                      A dimensionless ratio, so a number on the wire
 * @param observedContribution weight times the actual return - what happened.
 *                      Null whenever {@code priceChangePct} is
 * @param valueImpact   the money figure, and the only string here, so
 *                      JavaScript's double cannot round it (CLAUDE.md). Null
 *                      when the return is unknown
 * @param alignment     INCONCLUSIVE covers both a sub-epsilon move and missing
 *                      price data - a real answer, not a failure to report
 */
@Schema(description = "One story's assessed effect on one holding")
public record ImpactRow(
        String symbol,
        String companyName,
        Double holdingWeight,
        Double priceChangePct,
        Double expectedImpact,
        Double observedContribution,
        BigDecimal valueImpact,
        Direction direction,
        Alignment alignment) {
}
