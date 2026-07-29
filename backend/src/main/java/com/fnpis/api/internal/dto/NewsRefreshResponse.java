package com.fnpis.api.internal.dto;

/**
 * Response body for {@code POST /api/v1/news/refresh} (C6).
 *
 * @param triggered true if the refresh was accepted (false on 409 conflict)
 * @param fetched number of symbols whose news was fetched successfully
 * @param inserted new article-link rows created
 * @param skippedDuplicates articles skipped because they were already linked
 * @param failures persistence attempts that threw an exception
 */
public record NewsRefreshResponse(
        boolean triggered,
        int fetched,
        int inserted,
        int skippedDuplicates,
        int failures) {
}
