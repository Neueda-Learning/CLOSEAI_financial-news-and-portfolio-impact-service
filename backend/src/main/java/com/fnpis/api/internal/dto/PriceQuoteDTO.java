package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Current quote and day change for one symbol (B1).
 *
 * <p>Monetary values are serialized as strings per contract §1.2.
 */
@Schema(description = "Latest quote for one symbol")
public record PriceQuoteDTO(
        @Schema(example = "AAPL") String symbol,
        @Schema(example = "195.30") BigDecimal price,
        @Schema(example = "192.10") BigDecimal previousClose,
        @Schema(example = "3.20") BigDecimal change,
        @Schema(example = "1.67") BigDecimal changePct,
        @Schema(example = "true") boolean quoteAvailable,
        Instant asOf,
        boolean stale) {
}
