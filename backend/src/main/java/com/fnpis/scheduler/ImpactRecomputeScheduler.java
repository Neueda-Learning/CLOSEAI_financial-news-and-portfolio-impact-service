package com.fnpis.scheduler;

import com.fnpis.common.error.ApiException;
import com.fnpis.service.ImpactAssessmentService;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the impact recompute, on a timer and on demand (E4, architecture 5).
 *
 * <p>Last in the job chain: news lands, sentiment scores it, the closing snapshot
 * fills {@code price_bar}, then this reads all three. Running before the snapshot
 * is not an error - a session with no bar yet falls to the live-quote tier, and
 * failing that becomes INCONCLUSIVE (EC-18) - but it does mean the day's numbers
 * firm up once the snapshot lands, which is why the timer keeps running rather
 * than firing once.
 *
 * <p>{@code fixedDelay} covers self-overlap (EC-23); the service's lock covers an
 * admin trigger arriving on an HTTP thread mid-run.
 */
@Component
public class ImpactRecomputeScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImpactRecomputeScheduler.class);
    private final ImpactAssessmentService service;

    public ImpactRecomputeScheduler(ImpactAssessmentService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.schedule.impact-recompute-delay}")
    public void scheduledRecompute() {
        if (!service.tryAcquire()) {
            log.debug("Impact recompute skipped — already running");
            return;
        }
        try {
            service.recomputeFor(Instant.now());
        } finally {
            service.release();
        }
    }

    /**
     * Manual trigger for the admin endpoint.
     *
     * <p>Refuses rather than queues when a run is in flight (EC-20). Queueing
     * would be worse here than for the news poll: this overwrites a whole
     * session's assessments, so a queued duplicate rewrites the numbers on screen
     * a second time for no gain.
     *
     * @param portfolioId one portfolio, or null for every one of them
     * @param session     the session to attribute to
     * @return rows written or overwritten
     * @throws ApiException 409 when a recompute is already running
     */
    public int manualRecompute(Long portfolioId, LocalDate session) {
        if (!service.tryAcquire()) {
            throw ApiException.taskAlreadyRunning("impact-recompute");
        }
        try {
            Instant now = Instant.now();
            return portfolioId == null
                    ? service.recomputeFor(session, now)
                    : service.recomputeOne(portfolioId, session, now);
        } finally {
            service.release();
        }
    }
}
