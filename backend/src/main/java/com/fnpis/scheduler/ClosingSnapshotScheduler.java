package com.fnpis.scheduler;

import com.fnpis.service.ClosingSnapshotService;
import com.fnpis.service.ClosingSnapshotService;
import java.time.Clock;
import java.time.Instant;
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
    private final ClosingSnapshotService service;
    private final Clock clock;

    public ClosingSnapshotScheduler(ClosingSnapshotService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.schedule.closing-snapshot-cron:0 5 16 * * MON-FRI}",
               zone = "America/New_York")
    public void scheduledCapture() {
        log.info("Closing snapshot triggered at {}", Instant.now(clock));
        service.capture();
    }
}
