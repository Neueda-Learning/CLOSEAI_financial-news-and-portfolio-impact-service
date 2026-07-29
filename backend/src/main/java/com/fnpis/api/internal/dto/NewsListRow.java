package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * One row in the news list (C3, C4).
 */
@Schema(description = "News list item with optional sentiment")
public record NewsListRow(
        @Schema(example = "8842") Long id,
        @Schema(example = "Nvidia beats Q2 estimates, raises guidance") String headline,
        @Schema(example = "Reuters") String source,
        String url,
        Instant publishedAt,
        List<String> symbols,
        @Schema(description = "null when not yet analyzed") SentimentSummary sentiment,
        @Schema(example = "false") boolean hasImpact) {

    @Schema(description = "Sentiment verdict summary, null if not yet analyzed")
    public record SentimentSummary(
            @Schema(example = "POSITIVE") String label,
            @Schema(example = "0.72") Double score,
            @Schema(example = "0.88") Double confidence,
            String modelVersion) {
    }
}
