package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.NewsDetailResponse;
import com.fnpis.api.internal.dto.NewsListRow;
import com.fnpis.common.PagedResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.service.NewsReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only news endpoints (C3, C4, C5).
 *
 * <p>Never calls a provider — serves data already fetched by the scheduled
 * news poll (architecture decision 2). Sentiment may be null when the
 * analysis job has not yet processed the article.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "News", description = "新闻与情感")
public class NewsController {

    private final NewsReadService service;

    public NewsController(NewsReadService service) {
        this.service = service;
    }

    @GetMapping("/news")
    @Operation(summary = "新闻列表 (C3, C4)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated news list",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public PagedResponse<NewsListRow> list(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Instant fromTime = from != null ? LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().minus(java.time.Duration.ofDays(7));
        Instant toTime = to != null ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plus(java.time.Duration.ofDays(1));
        return service.list(symbol, sentiment, fromTime, toTime, page, size);
    }

    @GetMapping("/news/{id}")
    @Operation(summary = "新闻详情 (C5)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Article with sentiment and symbols",
                    content = @Content(schema = @Schema(implementation = NewsDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Article not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NewsDetailResponse detail(@PathVariable Long id) {
        NewsDetailResponse r = service.detail(id);
        if (r == null) {
            throw ApiException.articleNotFound(id);
        }
        return r;
    }
}
