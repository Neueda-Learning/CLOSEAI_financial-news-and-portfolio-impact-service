package com.fnpis.repository;

import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.SentimentLabel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByExternalId(String externalId);

    List<NewsArticle> findByPublishedAtBetween(Instant start, Instant end);

    /**
     * Filtered news list with optional symbol, sentiment, and date range (C3, C4).
     *
     * <p>The symbol filter joins through {@code article_security_link};
     * the sentiment filter joins through {@code sentiment_score}.
     * Both are optional — a null parameter disables that filter.
     */
    @Query("""
            SELECT DISTINCT a FROM NewsArticle a
            WHERE (:symbol IS NULL
                   OR EXISTS (SELECT 1 FROM ArticleSecurityLink l
                              WHERE l.articleId = a.id AND l.symbol = :symbol))
              AND (:sentiment IS NULL
                   OR EXISTS (SELECT 1 FROM SentimentScore s
                              WHERE s.articleId = a.id AND s.label = :sentiment))
              AND a.publishedAt BETWEEN :from AND :to
            ORDER BY a.publishedAt DESC""")
    Page<NewsArticle> findFiltered(
            @Param("symbol") String symbol,
            @Param("sentiment") SentimentLabel sentiment,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    /**
     * Articles with no {@code sentiment_score} row yet, oldest first (D3).
     *
     * <p>The analysis job's work queue. Written as a {@code NOT EXISTS} subquery
     * because the entities carry no associations by design (entity guide) - there
     * is no {@code article.sentiment} to navigate, and loading every article to
     * filter in memory would grow with the table rather than with the backlog.
     *
     * <p><b>Paged, and the caller must keep it that way.</b> Each row costs one
     * LLM call, so an unbounded result would hand the whole backlog to the
     * {@code llmSentiment} rate limiter in a single pass and spend the day's
     * quota on the first run.
     *
     * <p>Oldest first so a backlog drains in publication order: the story that
     * has been waiting longest is the one a reader is most likely to have already
     * seen unscored.
     */
    @Query("""
            select a from NewsArticle a
            where not exists (
                select 1 from SentimentScore s where s.articleId = a.id
            )
            order by a.publishedAt asc
            """)
    List<NewsArticle> findUnanalysed(Pageable page);
}
