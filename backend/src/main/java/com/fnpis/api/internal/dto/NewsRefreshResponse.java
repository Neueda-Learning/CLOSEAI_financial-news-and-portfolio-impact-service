package com.fnpis.api.internal.dto;

import java.time.Instant;

/**
 * Response body for {@code POST /api/v1/news/refresh} (C6).
 *
 * @param triggered UTC time the refresh was triggered
 * @param fetched number of symbols whose news was fetched successfully
 * @param inserted new article-link rows created
 * @param skippedDuplicates articles skipped because they were already linked
 */
public record NewsRefreshResponse(
        Instant triggered,
        int fetched,
        int inserted,
        int skippedDuplicates) {
}
