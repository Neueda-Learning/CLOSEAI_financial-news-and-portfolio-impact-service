package com.fnpis.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Response DTO for {@code GET /api/v1/quote?symbol=SYM}.
 *
 * <p>Must never appear in a {@code service/} method signature — that is the whole
 * point of the anti-corruption layer.
 *
 * @param t Unix timestamp of the quote (seconds)
 */
public record FinnhubQuoteResponse(
        @JsonProperty("c") BigDecimal c,
        @JsonProperty("h") BigDecimal h,
        @JsonProperty("l") BigDecimal l,
        @JsonProperty("o") BigDecimal o,
        @JsonProperty("pc") BigDecimal pc,
        @JsonProperty("t") long t) {
}
