package com.fnpis.repository;

import com.fnpis.domain.NewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByExternalId(String externalId);

    List<NewsArticle> findByPublishedAtBetween(Instant start, Instant end);

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
