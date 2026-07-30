package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.ImpactViewResponse;
import com.fnpis.service.ImpactViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The linked view (F4) - the contract calls this the project's most important
 * endpoint, and the demo's fourth step is this one request.
 *
 * <p>Mounted under {@code /news} because the story is the subject: the portfolio
 * arrives as a parameter since the same story produces different impact amounts
 * against different holdings.
 */
@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "Impact view", description = "新闻与价格联动视图 (F4)")
public class ImpactViewController {

    private final ImpactViewService service;

    ImpactViewController(ImpactViewService service) {
        this.service = service;
    }

    /**
     * Everything the linked page needs for one story.
     *
     * @param portfolioId required - a value impact has no meaning without the
     *                    holding weight that scales it
     * @param symbol      which curve to return; omitted picks the biggest mover
     */
    @GetMapping("/{id}/impact-view")
    @Operation(summary = "一次取全：新闻、情绪、受影响持仓与单只日内曲线 (F4)")
    public ImpactViewResponse impactView(
            @PathVariable Long id,
            @RequestParam Long portfolioId,
            @Parameter(description = "缺省时返回影响金额绝对值最大的那只")
            @RequestParam(required = false) String symbol,
            @Parameter(description = "intraday=单日分钟级, week=5天日线级")
            @RequestParam(required = false, defaultValue = "intraday") String range) {
        return service.view(id, portfolioId, symbol, "week".equals(range));
    }
}
