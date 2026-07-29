package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.SentimentRefreshResponse;
import com.fnpis.scheduler.SentimentAnalysisScheduler;
import com.fnpis.service.SentimentAnalysisService.AnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual trigger for the sentiment backlog (D3, Module D).
 *
 * <p>The timer runs every five minutes
 * ({@link SentimentAnalysisScheduler#scheduledAnalysis()}); this endpoint exists
 * so a demo or a test does not have to wait for it. Without it the only way to
 * score a freshly fetched headline was to restart with a shorter delay.
 *
 * <p>Not behind the admin guard, unlike {@code /impacts/recompute}. That one
 * overwrites a session's existing assessments; this one only fills in verdicts
 * for articles that have none, so a second call reaches the same state rather
 * than rewriting anything a reader has already seen. What it does spend is LLM
 * quota - capped per run by {@code app.sentiment.batch-size}.
 *
 * <p>A call arriving while a run is in flight returns 409 rather than queueing
 * (EC-20): the run is idempotent, so a queued duplicate would only spend quota
 * arriving at the same place.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Sentiment", description = "情感分析")
public class SentimentRefreshController {

    private final SentimentAnalysisScheduler scheduler;

    public SentimentRefreshController(SentimentAnalysisScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Operation(
            summary = "手动触发情感分析 (D3)",
            description = """
                    Scores up to one batch of headlines that have no sentiment_score row \
                    yet, oldest first. Verdicts failing validation are counted as \
                    rejected rather than stored. If an analysis is already running \
                    (scheduled or manual), returns 409 immediately — it never queues."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis accepted and completed",
                    content = @Content(
                            schema = @Schema(implementation = SentimentRefreshResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "An analysis is already running — try again later",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/sentiment/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SentimentRefreshResponse> triggerRefresh() {
        AnalysisResult r = scheduler.manualAnalysis();
        return ResponseEntity.ok(new SentimentRefreshResponse(
                r.triggered(), r.analysed(), r.stored(), r.rejected(), r.failures()));
    }
}
