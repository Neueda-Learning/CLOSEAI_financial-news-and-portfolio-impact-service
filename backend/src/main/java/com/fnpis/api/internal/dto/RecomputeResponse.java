package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Outcome of a manual recompute (API contract 4.4).
 *
 * <p>Carries who triggered it and when it finished because the contract's admin
 * section requires these operations to be auditable: this overwrites the day's
 * existing assessments, so "who ran this and when" has to survive in the
 * response even when nobody is tailing the logs.
 *
 * @param recomputed  rows written - inserted plus overwritten, since an upsert
 *                    does not distinguish them and the caller only cares how
 *                    much of the day changed
 * @param portfolioId the portfolio recomputed, null when the run covered all
 * @param triggeredBy the admin identity, never the token itself
 * @param completedAt UTC finish time
 */
@Schema(description = "Recompute result with audit fields")
public record RecomputeResponse(
        int recomputed,
        Long portfolioId,
        LocalDate date,
        String triggeredBy,
        Instant completedAt) {
}
