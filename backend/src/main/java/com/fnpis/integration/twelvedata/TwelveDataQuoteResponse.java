package com.fnpis.integration.twelvedata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Response DTO for {@code GET /quote?symbol=SYM}.
 */
public record TwelveDataQuoteResponse(
        @JsonProperty("symbol") String symbol,
        @JsonProperty("open") BigDecimal open,
        @JsonProperty("high") BigDecimal high,
        @JsonProperty("low") BigDecimal low,
        @JsonProperty("close") BigDecimal close,
        @JsonProperty("previous_close") BigDecimal previousClose,
        @JsonProperty("datetime") String datetime) {
}
