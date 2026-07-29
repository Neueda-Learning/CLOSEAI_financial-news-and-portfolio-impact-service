package com.fnpis.integration.twelvedata;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for {@code GET /time_series?symbol=SYM&interval=1day}.
 */
public record TwelveDataTimeSeriesResponse(
        @JsonProperty("meta") Meta meta,
        @JsonProperty("values") List<Value> values,
        @JsonProperty("status") String status) {

    public record Meta(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("interval") String interval,
            @JsonProperty("currency") String currency,
            @JsonProperty("exchange_timezone") String exchangeTimezone,
            @JsonProperty("exchange") String exchange,
            @JsonProperty("mic_code") String micCode,
            @JsonProperty("type") String type) {
    }

    public record Value(
            @JsonProperty("datetime") String datetime,
            @JsonProperty("open") BigDecimal open,
            @JsonProperty("high") BigDecimal high,
            @JsonProperty("low") BigDecimal low,
            @JsonProperty("close") BigDecimal close,
            @JsonProperty("volume") long volume) {
    }
}
