package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.SecurityOption;
import com.fnpis.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Security lookup for the add-holding form (A4, EC-05).
 *
 * <p>Read-only, and a plain list rather than a paged envelope. The contract
 * groups this under "securities and quotes" by path, but the requirement it
 * serves is A4 - it exists so the symbol field can offer the fifteen seeded
 * codes instead of making the user guess one and collect a 404.
 *
 * <p>Not paged on purpose: the result is a type-ahead dropdown, capped in the
 * service. Wrapping twenty suggestions in {@code PagedResponse} would hand the
 * frontend page arithmetic for a list nobody pages through.
 */
@RestController
@RequestMapping("/api/v1/securities")
@Tag(name = "Securities", description = "标的检索")
public class SecurityController {

    private final SecurityService service;

    SecurityController(SecurityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "标的搜索，新增持仓时选代码用 (A4, EC-05)")
    public List<SecurityOption> search(
            @Parameter(description = "代码前缀或公司名片段；留空返回全部自选股")
            @RequestParam(required = false) String q) {
        return service.search(q);
    }
}
