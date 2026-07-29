package com.fnpis.api.internal;

import com.fnpis.scheduler.QuoteRefreshScheduler;
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
 * Manual trigger for the quote refresh pipeline (Module B).
 *
 * <p>Scheduled at 1-minute intervals during US market hours
 * ({@link QuoteRefreshScheduler#scheduledRefresh()}), with this endpoint
 * as a manual override for demos and debugging (B6).
 * The scheduler acquires an {@code AtomicBoolean} lock; a second call
 * while a refresh is already running returns 409 rather than queueing.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quotes", description = "报价刷新")
public class QuoteRefreshController {

    private final QuoteRefreshScheduler scheduler;

    public QuoteRefreshController(QuoteRefreshScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Operation(
            summary = "手动触发报价刷新 (B6)",
            description = """
                    Fetches the latest quote for every symbol in the watchlist via the \
                    PriceProvider chain (Finnhub → Twelve Data → DB), then writes both \
                    price_quote (upsert) and price_point (append) in one transaction per symbol. \
                    Accepted immediately (202) — the refresh runs asynchronously. \
                    A second call while one is running returns 409."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Refresh accepted, running in background"),
            @ApiResponse(responseCode = "409", description = "A refresh is already running — try again later",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/quotes/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> triggerRefresh() {
        scheduler.manualRefresh();
        return ResponseEntity.accepted().build();
    }
}
