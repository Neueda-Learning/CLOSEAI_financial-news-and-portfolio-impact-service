package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio valuation summary (A2, B2, B3).
 *
 * <p>Feeds the overview screen's headline figures and the allocation pie. The
 * three totals are the acceptance target for SC-002 - a lecturer will check them
 * on a calculator, so they must be exact sums of the per-row values, not
 * separately rounded.
 *
 * <p>An empty portfolio is not an error (EC-01): totals come back as
 * {@code "0.00"} and {@code allocations} as an empty array, so the frontend can
 * render its empty state.
 *
 * @param unrealizedPnLPct null when total cost is zero - same dash rule as a row (EC-07)
 * @param dayChange        null when no holding has a previous close to compare against
 * @param asOf             oldest quote capture time behind these numbers, null when
 *                         no quote exists yet
 * @param stale            true when any quote is past its freshness budget, or
 *                         there are none at all (B4, SC-009)
 */
@Schema(description = "Portfolio totals and allocation breakdown")
public record PortfolioSummaryResponse(
        Long id,
        String name,
        String baseCurrency,
        BigDecimal totalMarketValue,
        BigDecimal totalCost,
        BigDecimal unrealizedPnL,
        Double unrealizedPnLPct,
        BigDecimal dayChange,
        Double dayChangePct,
        List<AllocationItem> allocations,
        Instant asOf,
        boolean stale) {
}
