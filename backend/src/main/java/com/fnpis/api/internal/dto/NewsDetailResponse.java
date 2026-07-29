package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Full news article with sentiment and associated symbols (C5).
 */
@Schema(description = "News article detail with sentiment")
public record NewsDetailResponse(
        @Schema(example = "8842") Long id,
        @Schema(example = "Nvidia beats Q2 estimates, raises guidance") String headline,
        @Schema(example = "Reuters") String source,
        String url,
        String summary,
        String image,
        Instant publishedAt,
        Instant fetchedAt,
        List<String> symbols,
        @Schema(description = "null when not yet analyzed") NewsListRow.SentimentSummary sentiment,
        @Schema(example = "false") boolean hasImpact) {
}
