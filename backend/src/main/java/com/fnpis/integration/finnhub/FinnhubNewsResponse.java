package com.fnpis.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for {@code GET /api/v1/company-news?symbol=SYM}.
 *
 * <p>Must never appear in a {@code service/} method signature.
 */
public record FinnhubNewsResponse(
        @JsonProperty("category") String category,
        @JsonProperty("datetime") long datetime,
        @JsonProperty("headline") String headline,
        @JsonProperty("id") long id,
        @JsonProperty("image") String image,
        @JsonProperty("related") String related,
        @JsonProperty("source") String source,
        @JsonProperty("summary") String summary,
        @JsonProperty("url") String url) {
}
