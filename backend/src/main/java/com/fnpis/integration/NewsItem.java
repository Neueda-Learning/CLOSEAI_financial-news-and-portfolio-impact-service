package com.fnpis.integration;

import java.time.Instant;

/**
 * What a news provider fetched — not an {@code @Entity}.
 *
 * <p>Returned by {@link NewsProvider#fetchCompanyNews}. The service layer converts
 * this into a {@link com.fnpis.domain.NewsArticle} before persisting.
 *
 * @param summary Provider summary. Persisted for future use; sentiment analyses
 *                the headline only (AS-04).
 */
public record NewsItem(
        String externalId,
        String headline,
        String source,
        String url,
        String summary,
        String image,
        Instant publishedAt) {
}
