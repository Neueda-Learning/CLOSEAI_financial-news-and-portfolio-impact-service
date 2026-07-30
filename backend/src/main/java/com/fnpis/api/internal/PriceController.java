package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.PriceQuoteDTO;
import com.fnpis.api.internal.dto.ValuationHistoryResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.service.PriceReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only price and valuation endpoints (B1, B2, F5).
 *
 * <p>Never calls a provider — serves data already fetched by scheduled refresh jobs
 * (architecture decision 2). When no data exists, returns what is available with
 * clear staleness markers rather than errors.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Prices", description = "行情与估值")
public class PriceController {

    private final PriceReadService service;

    public PriceController(PriceReadService service) {
        this.service = service;
    }

    @GetMapping("/prices/{symbol}/quote")
    @Operation(summary = "最新报价 (B1)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quote with freshness",
                    content = @Content(schema = @Schema(implementation = PriceQuoteDTO.class))),
            @ApiResponse(responseCode = "404", description = "Symbol not found in security table",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PriceQuoteDTO quote(@PathVariable String symbol) {
        PriceQuoteDTO dto = service.quote(symbol, Instant.now());
        if (dto == null) {
            throw ApiException.securityNotFound(symbol);
        }
        return dto;
    }

    @GetMapping("/portfolios/{portfolioId}/valuation-history")
    @Operation(summary = "组合价值走势 (B2, F5)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value points for charting",
                    content = @Content(schema = @Schema(implementation = ValuationHistoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ValuationHistoryResponse valuationHistory(
            @PathVariable Long portfolioId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String symbol) {
        LocalDate toDate = to != null ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = from != null ? LocalDate.parse(from) : toDate.minusDays(30);
        ValuationHistoryResponse r;
        if (symbol != null && !symbol.isBlank()) {
            r = service.symbolHistory(portfolioId, symbol, fromDate, toDate);
            if (r == null) {
                throw ApiException.securityNotFound(symbol);
            }
        } else {
            r = service.valuationHistory(portfolioId, fromDate, toDate);
            if (r == null) {
                throw ApiException.portfolioNotFound(portfolioId);
            }
        }
        return r;
    }
}
