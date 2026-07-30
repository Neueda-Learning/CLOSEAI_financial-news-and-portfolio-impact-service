package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * One row of the holdings list (A5, API contract holdings response).
 *
 * <p><b>BigDecimal fields serialise as JSON strings, Double fields as numbers.</b>
 * That split is the contract (1.2), enforced globally by {@code JacksonConfig}:
 * money and quantities are strings so the frontend cannot lose precision doing
 * float arithmetic on them (SC-002), while percentages and weights stay numbers
 * because they are only ever displayed. Typing a percentage as BigDecimal here
 * would emit {@code "3.46"} with quotes and break the agreed shape.
 *
 * @param unrealizedPnLPct null when cost basis is zero (EC-07) - the frontend
 *                         shows a dash. Not Infinity, not NaN, not zero.
 * @param dayChange        null when the quote carries no previous close (EC-19)
 * @param quoteAvailable   false when the symbol has no fresh quote (EC-13);
 *                         {@code currentPrice} is then the last known price, or
 *                         null if none was ever captured
 */
@Schema(description = "A position with its current valuation")
public record HoldingRow(
        Long id,
        String symbol,
        String companyName,
        BigDecimal quantity,
        BigDecimal costBasis,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal marketValue,
        BigDecimal totalCost,
        BigDecimal unrealizedPnL,
        Double unrealizedPnLPct,
        BigDecimal dayChange,
        Double dayChangePct,
        Double weight,
        boolean quoteAvailable) {
}
