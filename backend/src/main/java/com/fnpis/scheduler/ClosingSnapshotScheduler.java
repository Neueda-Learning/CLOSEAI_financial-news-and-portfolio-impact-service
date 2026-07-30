package com.fnpis.scheduler;

import com.fnpis.service.ClosingSnapshotService;
import com.fnpis.service.ValuationSnapshotService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs once per trading day, shortly after the US market close (16:05 ET).
 *
 * <p>Does not check weekends or holidays — the scheduler runs every weekday
 * at the configured time. On non-trading days Twelve Data returns the last
 * available bar (duplicate writes are idempotent because the composite key
 * is (symbol, date)).
 */
@Component
public class ClosingSnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClosingSnapshotScheduler.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    private final ClosingSnapshotService service;
    private final ValuationSnapshotService valuations;
    private final Clock clock;

    public ClosingSnapshotScheduler(
            ClosingSnapshotService service, ValuationSnapshotService valuations, Clock clock) {
        this.service = service;
        this.valuations = valuations;
        this.clock = clock;
    }

    /**
     * Bars first, then portfolio valuations.
     *
     * <p>Order matters only in that both must run: architecture 5 gives this job
     * both writes, and the valuation half was missing, leaving F5 with no data
     * source at all. The valuation is taken from stored quotes rather than the
     * bars just written, so it does not depend on the first half succeeding - and
     * it runs even if that half threw, because an unrecorded session is lost for
     * good.
     */
    @Scheduled(cron = "${app.schedule.closing-snapshot-cron:0 5 16 * * MON-FRI}",
               zone = "America/New_York")
    public void scheduledCapture() {
        Instant now = Instant.now(clock);
        log.info("Closing snapshot triggered at {}", now);
        try {
            service.capture();
        } catch (RuntimeException e) {
            log.warn("Bar capture failed - taking the valuation snapshot anyway ({})",
                    e.getClass().getSimpleName());
        }
        valuations.captureAll(LocalDate.ofInstant(now, MARKET_ZONE), now);
    }
}
