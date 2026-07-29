package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A portfolio as returned by the list and create endpoints (A1, A2).
 *
 * <p>A2's acceptance criterion is "list shows name and total value", so the
 * total is carried here rather than making the frontend call
 * {@code /summary} once per row.
 *
 * @param totalMarketValue current value, {@code "0.00"} for an empty portfolio (EC-01)
 * @param holdingCount     lets the frontend show "no positions yet" without a second call (EC-04)
 * @param stale            true when the valuation rests on stale or missing quotes
 */
@Schema(description = "A portfolio with its current total value")
public record PortfolioResponse(
        Long id,
        String name,
        String baseCurrency,
        BigDecimal totalMarketValue,
        long holdingCount,
        Instant createdAt,
        Instant asOf,
        boolean stale) {
}
