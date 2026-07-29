package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Body of {@code POST /api/v1/impacts/recompute} (API contract 4.4).
 *
 * <p>Both fields are optional. Omitting {@code portfolioId} recomputes every
 * portfolio, which is what the scheduled run does; omitting {@code date} uses
 * the current session. Neither is validated here beyond nullability - whether a
 * portfolio exists is the service's question, not the binder's.
 *
 * @param portfolioId one portfolio, or null for all of them
 * @param date        the session to attribute to, or null for today
 */
@Schema(description = "Which portfolio and session to recompute")
public record RecomputeRequest(
        Long portfolioId,
        LocalDate date) {
}
