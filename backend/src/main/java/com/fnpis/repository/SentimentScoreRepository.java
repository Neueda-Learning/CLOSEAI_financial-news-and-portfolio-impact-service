package com.fnpis.repository;

import com.fnpis.domain.SentimentScore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Sentiment verdicts (D1, D2). Module D writes; module E reads to feed the
 * impact engine (requirements 5.3 step 2).
 *
 * <p>The read is always by article: an impact assessment starts from a story,
 * asks the sentiment for it, and skips the story when there is none. The unique
 * constraint {@code uq_sentiment_article} makes at most one row per article, so
 * {@link Optional} is the honest return type - an empty result means the
 * analysis job has not run for this headline yet, which is the EC-13-style
 * "skip, do not fabricate" path.
 */
public interface SentimentScoreRepository extends JpaRepository<SentimentScore, Long> {

    /** The verdict for one article, empty when it has not been analysed. */
    Optional<SentimentScore> findByArticleId(Long articleId);
}
