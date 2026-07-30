package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One financial news item (C1). Only the headline is analysed - article bodies
 * are out of scope (AS-04).
 *
 * <p>Deduplication is entirely {@link #externalId}, which carries a unique
 * constraint. Insert with an upsert or a check-then-insert so the 15-minute
 * poll cannot land the same story twice (C2, SC-005).
 */
@Entity
@Table(name = "news_article")
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    /** Column length; the provider id must fit without truncation. */
    public static final int EXTERNAL_ID_MAX = 128;

    /** Headlines longer than this are truncated rather than stored as TEXT. */
    public static final int HEADLINE_MAX = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Provider-side unique id. Unique in the schema - the whole dedup rule. */
    @Column(name = "external_id", nullable = false, length = EXTERNAL_ID_MAX)
    private String externalId;

    @Column(name = "headline", nullable = false, length = HEADLINE_MAX)
    private String headline;

    /**
     * Provider summary. Stored but not analysed - sentiment reads the headline
     * only (AS-04). Null when the provider returned none.
     *
     * <p>Kept because it cannot be recovered later: once an article falls out of
     * the free tier's history window the summary is gone, same as
     * {@code price_point} rows.
     */
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    /**
     * Thumbnail URL from the provider, or null when the article has no image.
     *
     * <p>TEXT rather than {@code url}'s VARCHAR(1024): CDN URLs carry signing
     * and resize parameters and run past that limit, and a truncated URL is
     * broken rather than merely shortened.
     */
    @Column(name = "image", columnDefinition = "TEXT")
    private String image;

    /** UTC. Drives the attribution date (EC-16, EC-17). */
    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    /**
     * When we fetched it, as opposed to when it was published. UTC.
     *
     * <p>The news list reports {@code asOf} from this column. Deriving
     * freshness from {@link #publishedAt} would mark a story fetched a minute
     * ago as stale because it was written yesterday.
     */
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @PrePersist
    void stampFetchedAt() {
        if (fetchedAt == null) {
            fetchedAt = Instant.now();
        }
    }
}
