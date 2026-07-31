package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.ImpactRow;
import com.fnpis.api.internal.dto.ImpactSummaryResponse;
import com.fnpis.common.PagedResponse;
import com.fnpis.domain.Alignment;
import com.fnpis.service.ImpactQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Assessed impacts for a portfolio (E1-E3, E5).
 *
 * <p>Read-only and served entirely from {@code impact_assessment} (decision 3),
 * so nothing here can fail because a provider is down. Validation and DTO
 * shaping only - every figure is computed by the recompute job and every rule
 * lives in {@link ImpactQueryService} (CLAUDE.md layering table).
 *
 * <p>Mapped under {@code /portfolios} because both endpoints are scoped to one:
 * an impact has no meaning without the holdings that give it a weight.
 */
@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Impacts", description = "影响评估读接口")
public class ImpactController {

    private final ImpactQueryService service;

    ImpactController(ImpactQueryService service) {
        this.service = service;
    }

    /**
     * One session's impacts, newest first, optionally narrowed to one alignment.
     *
     * @param date      session to read, defaults to today when omitted
     * @param alignment CONFIRMED / DIVERGENT / INCONCLUSIVE, or omitted for all
     * @param page      1-based, per the paging envelope
     */
    @GetMapping("/{id}/impacts")
    @Operation(summary = "影响列表，可按对齐结果筛选 (E1~E3)")
    public PagedResponse<ImpactRow> impacts(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) Alignment alignment,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size) {
        return service.list(id, orToday(date), alignment, page, size);
    }

    /**
     * One session's roll-up, including the direction agreement rate demo script
     * step 5 shows (E5).
     */
    @GetMapping("/{id}/impact-dates")
    @Operation(summary = "该组合有影响数据的所有交易日")
    public List<LocalDate> impactDates(@PathVariable Long id) {
        return service.availableDates(id);
    }

    @GetMapping("/{id}/impact-summary")
    @Operation(summary = "日度汇总，含方向一致率与样本数 (E5)")
    public ImpactSummaryResponse summary(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) String symbol) {
        return service.summary(id, orToday(date), symbol);
    }

    /**
     * The contract's default for both endpoints' {@code date}.
     *
     * <p>Resolved here rather than in the service so the service always receives
     * an explicit session and stays testable without a clock.
     */
    private LocalDate orToday(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }
}
