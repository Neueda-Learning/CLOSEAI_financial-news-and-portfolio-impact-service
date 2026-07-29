package com.fnpis.scheduler;

import com.fnpis.common.error.ApiException;
import com.fnpis.service.SentimentAnalysisService;
import com.fnpis.service.SentimentAnalysisService.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the sentiment backlog on a timer (D3, architecture 5).
 *
 * <p>Second in the job chain: news poll lands articles, this scores them, the
 * closing snapshot fills {@code price_bar}, then the impact recompute reads both.
 * It is not chained off the poll's completion - it only looks for articles with
 * no verdict, so running independently reaches the same state and a failed poll
 * cannot stall scoring of what already landed.
 *
 * <p>{@code fixedDelay}, so a run starts only after the previous finished. That
 * alone stops the job overlapping itself (EC-23); the service's lock covers the
 * remaining case, a manual trigger arriving on an HTTP thread mid-run.
 */
@Component
public class SentimentAnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisScheduler.class);
    private final SentimentAnalysisService service;

    public SentimentAnalysisScheduler(SentimentAnalysisService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.schedule.sentiment-delay}")
    public void scheduledAnalysis() {
        if (!service.tryAcquire()) {
            log.debug("Sentiment analysis skipped — already running");
            return;
        }
        try {
            service.analyseBacklog();
        } finally {
            service.release();
        }
    }

    /**
     * Manual trigger via HTTP. Skipping rather than queueing is correct here: the
     * run is idempotent (it re-reads whatever still has no verdict), so a queued
     * duplicate would only spend LLM quota reaching the same state (EC-20).
     */
    public AnalysisResult manualAnalysis() {
        if (!service.tryAcquire()) {
            throw ApiException.taskAlreadyRunning("sentiment-analysis");
        }
        try {
            return service.analyseBacklog();
        } finally {
            service.release();
        }
    }
}
