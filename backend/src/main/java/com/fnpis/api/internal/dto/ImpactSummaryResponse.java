package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * One session's impact roll-up for a portfolio (E5, API contract 4.3).
 *
 * <p>Backs demo script step 5. {@code directionAgreementRate} is the headline
 * number there, and {@code sampleSize} travels with it deliberately: SC-008
 * requires the rate to rest on at least 20 records, so a lecturer seeing "71%"
 * without the sample behind it has no way to judge it. The frontend is required
 * to render both.
 *
 * @param weightedSentiment  portfolio-weighted sentiment across the session's
 *                           stories, null when nothing was assessed
 * @param newsCoverage       share of held symbols that had at least one story,
 *                           0-1, null when the portfolio holds nothing
 * @param directionAgreementRate confirmed / (confirmed + divergent), 0-1.
 *                           <b>Null when the sample is too small to mean
 *                           anything</b> (SC-008) - the frontend shows "sample
 *                           too small" rather than a number nobody should trust.
 *                           INCONCLUSIVE rows are excluded from the denominator:
 *                           they are not disagreements, they are absences of a
 *                           verdict
 * @param sampleSize         rows the rate is computed from - confirmed plus
 *                           divergent, not the total row count
 * @param topImpacted        biggest movers by absolute value impact, sign kept
 *                           so a loss stays visibly a loss
 */
@Schema(description = "Daily impact roll-up: agreement rate, coverage, top movers")
public record ImpactSummaryResponse(
        LocalDate date,
        Double weightedSentiment,
        Double newsCoverage,
        Double directionAgreementRate,
        int sampleSize,
        AlignmentCounts counts,
        List<TopImpactedItem> topImpacted,
        Instant asOf,
        boolean stale) {

    /**
     * How the session's rows split across the three alignments.
     *
     * <p>{@code confirmed + divergent} is {@link #sampleSize};
     * {@code inconclusive} sits outside it, which is why it is reported
     * separately rather than folded into a single total.
     */
    @Schema(description = "Row counts per alignment")
    public record AlignmentCounts(int confirmed, int divergent, int inconclusive) {
    }
}
