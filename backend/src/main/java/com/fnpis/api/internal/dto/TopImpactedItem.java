package com.fnpis.api.internal.dto;

import com.fnpis.domain.Alignment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * A session's biggest mover, for the summary's {@code topImpacted} list
 * (API contract 4.3).
 *
 * <p>Ranked by absolute value impact but reported with the sign intact - the
 * ranking asks "what moved the portfolio most", the display still has to show
 * whether it moved up or down.
 *
 * @param valueImpact money, serialized as a string (CLAUDE.md)
 */
@Schema(description = "A top mover by absolute value impact")
public record TopImpactedItem(
        String symbol,
        BigDecimal valueImpact,
        Alignment alignment) {
}
