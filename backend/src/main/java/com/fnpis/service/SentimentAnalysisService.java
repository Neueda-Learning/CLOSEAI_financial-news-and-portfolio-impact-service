package com.fnpis.service;

import com.fnpis.domain.NewsArticle;
import com.fnpis.integration.SentimentEngine;
import com.fnpis.integration.SentimentResult;
import com.fnpis.repository.NewsArticleRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Drives the sentiment engine over the backlog of unanalysed headlines (D3).
 *
 * <p>The missing link between module D's engine and module E's assessment: the
 * engine judges one headline and the impact service reads
 * {@code sentiment_score}, so without this loop the table stays empty and every
 * assessment skips for want of a verdict.
 *
 * <p><b>Bounded per run.</b> Each article costs one LLM call, so a run takes a
 * page of the backlog rather than all of it. That keeps one run's spend
 * predictable and lets the {@code llmSentiment} rate limiter shape the pace
 * instead of being handed the whole table at once. A backlog longer than one page
 * drains over successive runs, oldest first.
 *
 * <p><b>Failures are per-article and never poison the row.</b> Three outcomes
 * beyond success, each deliberate:
 * <ul>
 *   <li><b>Transport failure</b> - the engine throws (it refuses to fabricate a
 *       NEUTRAL on a blip, since the verdict would be permanent). Logged and
 *       skipped, so the next run retries the same article.</li>
 *   <li><b>Validation rejection</b> - the verdict was parseable but out of
 *       contract. Nothing is stored, and the article stays in the queue.</li>
 *   <li><b>A blank headline</b> - nothing to judge, so no call is paid for.</li>
 * </ul>
 * In all three the article keeps no row, which is what makes it eligible again.
 * That is a deliberate trade: a headline the model reliably refuses will be
 * retried every run, which shows up as a flat failure count in the log rather
 * than as silent data loss.
 */
@Service
public class SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);

    private final NewsArticleRepository articles;
    private final SentimentEngine engine;
    private final SentimentResultValidator validator;
    private final SentimentPersistenceService persistence;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** How many headlines one run will pay for. */
    private final int batchSize;

    SentimentAnalysisService(
            NewsArticleRepository articles,
            SentimentEngine engine,
            SentimentResultValidator validator,
            SentimentPersistenceService persistence,
            @Value("${app.sentiment.batch-size}") int batchSize) {
        this.articles = articles;
        this.engine = engine;
        this.validator = validator;
        this.persistence = persistence;
        this.batchSize = batchSize;
    }

    /**
     * Claims the right to run. Same shape as module BC's poll: the lock lives
     * here rather than in the scheduler so a manual HTTP trigger and the timer
     * contend for one lock instead of two (EC-20, EC-23).
     */
    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    public void release() {
        running.set(false);
    }

    /** What one analysis run did. {@code triggered} is false only when skipped. */
    public record AnalysisResult(
            boolean triggered, int analysed, int stored, int rejected, int failures) {}

    /**
     * Analyses up to one batch of unanalysed headlines.
     *
     * <p>Not {@code @Transactional}: the run makes one slow network call per
     * article, and a transaction spanning them would hold a connection for the
     * whole batch. {@link SentimentPersistenceService} owns the per-article
     * boundary instead.
     */
    public AnalysisResult analyseBacklog() {
        List<NewsArticle> backlog = articles.findUnanalysed(PageRequest.of(0, batchSize));
        if (backlog.isEmpty()) {
            log.debug("Sentiment analysis found no unanalysed articles");
            return new AnalysisResult(true, 0, 0, 0, 0);
        }

        int stored = 0;
        int rejected = 0;
        int failures = 0;
        for (NewsArticle article : backlog) {
            switch (analyseOne(article)) {
                case STORED -> stored++;
                case REJECTED -> rejected++;
                case FAILED -> failures++;
                default -> { /* already had a verdict; a concurrent run won */ }
            }
        }

        log.info("Sentiment analysis complete: {} analysed, {} stored, {} rejected, {} failures",
                backlog.size(), stored, rejected, failures);
        return new AnalysisResult(true, backlog.size(), stored, rejected, failures);
    }

    private enum Outcome { STORED, DUPLICATE, REJECTED, FAILED }

    private Outcome analyseOne(NewsArticle article) {
        String headline = article.getHeadline();
        if (headline == null || headline.isBlank()) {
            // The interface forbids a null or blank headline, and there is nothing
            // to judge anyway - no reason to pay for the call to find out.
            log.warn("Article {} has no headline, skipping", article.getId());
            return Outcome.REJECTED;
        }

        SentimentResult raw;
        try {
            raw = engine.analyze(headline);
        } catch (Exception e) {
            // Transport failure after Resilience4j exhausted its retries. The
            // class name only: a provider message can quote the headline back,
            // which is untrusted third-party text.
            log.warn("Sentiment engine failed for article {} ({}), leaving it queued",
                    article.getId(), e.getClass().getSimpleName());
            return Outcome.FAILED;
        }

        Optional<SentimentResult> valid = validator.validate(raw);
        if (valid.isEmpty()) {
            // Out of contract. Headline length rather than the headline, and no
            // model text at all - either could carry an injection payload.
            log.warn("Sentiment verdict rejected for article {} (headline {} chars)",
                    article.getId(), headline.length());
            return Outcome.REJECTED;
        }

        return persistence.persist(article.getId(), valid.get(), engine.modelVersion())
                ? Outcome.STORED
                : Outcome.DUPLICATE;
    }
}
