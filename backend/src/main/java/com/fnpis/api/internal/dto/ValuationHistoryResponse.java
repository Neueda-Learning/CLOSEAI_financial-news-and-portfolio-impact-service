package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio value over time (F5).
 *
 * <p>Points may be empty on a fresh install — the frontend must render
 * "collecting data" rather than an error (contract §4).
 */
@Schema(description = "Portfolio value history for charting")
public record ValuationHistoryResponse(
        @Schema(example = "1") Long portfolioId,
        List<ValuationPoint> points,
        Instant asOf,
        boolean stale) {

    @Schema(description = "One snapshot point")
    public record ValuationPoint(
            @Schema(example = "2026-07-27") String date,
            @Schema(example = "128450.75") BigDecimal totalValue) {
    }
}
