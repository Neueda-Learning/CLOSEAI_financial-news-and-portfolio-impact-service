package com.fnpis.api.internal.dto;

/**
 * Response body for {@code POST /api/v1/news/refresh} (C6).
 */
public record NewsRefreshResponse(int inserted, int skippedDuplicates) {
}
