package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.CreatePortfolioRequest;
import com.fnpis.api.internal.dto.PortfolioResponse;
import com.fnpis.api.internal.dto.PortfolioSummaryResponse;
import com.fnpis.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portfolios (A1-A3) and valuation summary (B2, B3).
 *
 * <p>Validation and DTO shaping only - all arithmetic and every rule lives in
 * the service layer (CLAUDE.md layering table). Errors are never built here
 * either: throwing {@code ApiException} lets {@code GlobalExceptionHandler}
 * produce one consistent problem+json skeleton (contract 6.1).
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolios", description = "组合与估值")
public class PortfolioController {

    private final PortfolioService service;

    PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建组合 (A1)")
    public PortfolioResponse create(@Valid @RequestBody CreatePortfolioRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "组合列表，含总市值 (A2)")
    public List<PortfolioResponse> list() {
        return service.list(Instant.now());
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "估值汇总与配置分布 (A2, B2, B3)")
    public PortfolioSummaryResponse summary(@PathVariable Long id) {
        return service.summary(id, Instant.now());
    }

    @GetMapping("/{id}")
    @Operation(summary = "组合详情，与 summary 同形 (A2)")
    public PortfolioSummaryResponse detail(@PathVariable Long id) {
        return service.summary(id, Instant.now());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除组合，持仓随之删除 (A3)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
