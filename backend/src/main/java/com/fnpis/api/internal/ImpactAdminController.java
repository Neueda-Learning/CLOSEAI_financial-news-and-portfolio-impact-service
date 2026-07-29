package com.fnpis.api.internal;

import com.fnpis.api.internal.dto.RecomputeRequest;
import com.fnpis.api.internal.dto.RecomputeResponse;
import com.fnpis.scheduler.ImpactRecomputeScheduler;
import com.fnpis.service.AdminTokenGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one destructive impact endpoint (E4, API contract 4.4).
 *
 * <p>Separate from {@link ImpactController} for two reasons. The contract mounts
 * it at {@code /impacts/recompute} rather than under a portfolio, since a run
 * with no {@code portfolioId} covers all of them; and keeping it apart means the
 * read endpoints cannot accidentally inherit an admin dependency, or the guard
 * quietly stop applying to this one.
 *
 * <p>Off by default. While {@code app.admin.enabled} is false every call here
 * answers 404, so the endpoint does not advertise that it exists.
 */
@RestController
@RequestMapping("/api/v1/impacts")
@Tag(name = "Impacts (admin)", description = "影响重算，需管理员令牌")
public class ImpactAdminController {

    private final ImpactRecomputeScheduler scheduler;
    private final AdminTokenGuard guard;

    ImpactAdminController(ImpactRecomputeScheduler scheduler, AdminTokenGuard guard) {
        this.scheduler = scheduler;
        this.guard = guard;
    }

    /**
     * Recomputes a session's assessments, overwriting what is there.
     *
     * <p>Authorisation comes first, before the body is looked at: an unauthorised
     * caller should not be able to tell a valid request from an invalid one.
     *
     * @param token the {@code X-Admin-Token} header
     * @return the row count plus audit fields - who triggered it and when it
     *         finished, since this rewrites numbers a reader may already have seen
     */
    @PostMapping("/recompute")
    @Operation(summary = "重算某日影响评估，覆盖既有结果 (E4)")
    public RecomputeResponse recompute(
            @RequestHeader(name = "X-Admin-Token", required = false) String token,
            @RequestBody(required = false) RecomputeRequest request) {
        String identity = guard.authorise(token);

        // An absent body is a valid "recompute everything for today" call, so it
        // is normalised rather than rejected.
        RecomputeRequest body = request == null ? new RecomputeRequest(null, null) : request;
        LocalDate session = body.date() == null ? LocalDate.now() : body.date();

        int recomputed = scheduler.manualRecompute(body.portfolioId(), session);

        return new RecomputeResponse(
                recomputed, body.portfolioId(), session, identity, Instant.now());
    }
}
