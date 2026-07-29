package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.AddHoldingRequest;
import com.fnpis.api.internal.dto.HoldingRow;
import com.fnpis.api.internal.dto.UpdateHoldingRequest;
import com.fnpis.common.PagedResponse;
import com.fnpis.service.HoldingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Positions (A4-A7).
 *
 * <p>Two base paths on purpose, matching the contract: adding and listing hang
 * off a portfolio, while patch and delete address a holding by its own id.
 */
@RestController
@Tag(name = "Holdings", description = "持仓管理")
public class HoldingController {

    private final HoldingService service;

    HoldingController(HoldingService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/portfolios/{portfolioId}/holdings")
    @Operation(summary = "持仓列表，含市值与盈亏 (A5)")
    public PagedResponse<HoldingRow> list(
            @PathVariable Long portfolioId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return service.list(portfolioId, page, size, Instant.now());
    }

    /** Repeat symbols merge rather than fail, so this answers 200, not 201 (EC-09). */
    @PostMapping("/api/v1/portfolios/{portfolioId}/holdings")
    @Operation(summary = "新增持仓；重复代码合并为一笔并重算加权均价 (A4, EC-09)")
    public HoldingRow add(
            @PathVariable Long portfolioId,
            @Valid @RequestBody AddHoldingRequest request) {
        return service.add(portfolioId, request, Instant.now());
    }

    @PatchMapping("/api/v1/holdings/{id}")
    @Operation(summary = "修改数量或成本价，估值随之重算 (A7)")
    public HoldingRow update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHoldingRequest request) {
        return service.update(id, request, Instant.now());
    }

    @DeleteMapping("/api/v1/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除持仓 (A6)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
