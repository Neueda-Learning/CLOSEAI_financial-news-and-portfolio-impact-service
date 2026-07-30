package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.NewsRefreshResponse;
import com.fnpis.scheduler.NewsFetchScheduler;
import com.fnpis.service.NewsFetchService.FetchResult;
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
 * Manual trigger for the news fetch pipeline (Module C).
 *
 * <p>Scheduled at 15-minute intervals ({@link NewsFetchScheduler#scheduledFetch()}),
 * with this endpoint as a manual override for demos and debugging (C6).
 * A second call while a fetch is running returns 409 rather than queueing.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "News", description = "新闻聚合")
public class NewsFetchController {

    private final NewsFetchScheduler scheduler;

    public NewsFetchController(NewsFetchScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Operation(
            summary = "手动触发新闻刷新 (C6)",
            description = """
                    Fetches company news for every symbol in the watchlist from Finnhub, \
                    persists new articles and their symbol links. Already-linked articles \
                    are counted as skippedDuplicates. If a fetch is already running \
                    (scheduled or manual), returns 409 immediately — it never queues."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refresh accepted and completed",
                    content = @Content(schema = @Schema(implementation = NewsRefreshResponse.class))),
            @ApiResponse(responseCode = "409", description = "A refresh is already running — try again later",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/news/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NewsRefreshResponse> triggerRefresh() {
        FetchResult r = scheduler.manualFetch();
        return ResponseEntity.ok(new NewsRefreshResponse(
                r.triggered(), r.fetched(), r.inserted(), r.skippedDuplicates(), r.failures()));
    }
}
