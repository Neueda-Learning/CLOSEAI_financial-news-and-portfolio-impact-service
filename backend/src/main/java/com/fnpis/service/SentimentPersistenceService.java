package com.fnpis.service;

import com.fnpis.domain.SentimentScore;
import com.fnpis.integration.SentimentResult;
import com.fnpis.repository.SentimentScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one validated verdict, in its own transaction (D3).
 *
 * <p>A separate bean from {@link SentimentAnalysisService} on purpose: Spring's
 * {@code @Transactional} works through a proxy, so a self-call from the loop in
 * that class would run with no transaction at all and the per-article boundary
 * would silently not exist. Module BC's news poll splits
 * {@link NewsPersistenceService} off for the same reason.
 *
 * <p>One transaction per article, not one for the batch: an LLM call is slow, and
 * a batch-wide transaction would hold a write connection open across all of them
 * and lose every verdict already paid for if the last one failed.
 */
@Service
public class SentimentPersistenceService {

    private final SentimentScoreRepository scores;

    SentimentPersistenceService(SentimentScoreRepository scores) {
        this.scores = scores;
    }

    /**
     * Stores the verdict for one article, or leaves an existing row alone.
     *
     * <p>Insert-only, unlike the impact upsert. {@code sentiment_score.article_id}
     * is unique and a verdict is written once and read forever (entity javadoc):
     * an LLM is not bit-reproducible even at temperature 0, so overwriting would
     * make the same story's score change between reads and break the
     * reproducibility that persisting it is there to provide.
     *
     * @return true when a row was inserted, false when the article already had a
     *         verdict - the caller counts the second case as a skip, not a
     *         failure, since a concurrent run getting there first is harmless
     */
    @Transactional
    public boolean persist(Long articleId, SentimentResult verdict, String modelVersion) {
        if (scores.findByArticleId(articleId).isPresent()) {
            return false;
        }

        SentimentScore row = new SentimentScore();
        row.setArticleId(articleId);
        row.setLabel(verdict.label());
        row.setScore(verdict.score());
        row.setConfidence(verdict.confidence());
        // The engine's version, not a constant: after a model or prompt change
        // this column is the only way to tell which one produced the verdict.
        row.setModelVersion(modelVersion);
        // analyzed_at is stamped by @PrePersist - the row is never updated, so
        // there is no stale-timestamp case to correct here.
        scores.save(row);
        return true;
    }
}
