package com.fnpis.scheduler;

import com.fnpis.common.error.ApiException;
import com.fnpis.service.QuoteRefreshService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuoteRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuoteRefreshScheduler.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private final QuoteRefreshService service;
    private final Clock clock;

    public QuoteRefreshScheduler(QuoteRefreshService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.schedule.quote-refresh-delay}")
    public void scheduledRefresh() {
        if (!isTradingHours(Instant.now(clock))) {
            return;
        }
        doRefresh();
    }

    /** Manual trigger via HTTP. Returns 409 if a refresh is already running. */
    public void manualRefresh() {
        if (!service.tryAcquire()) {
            throw ApiException.taskAlreadyRunning("quote-refresh");
        }
        try {
            doRefresh();
        } finally {
            service.release();
        }
    }

    boolean isTradingHours(Instant now) {
        ZonedDateTime et = now.atZone(MARKET_ZONE);
        if (et.getDayOfWeek().getValue() > 5) { // Saturday = 6, Sunday = 7
            return false;
        }
        int hour = et.getHour();
        int minute = et.getMinute();
        return (hour > 9 || (hour == 9 && minute >= 30)) && hour < 16;
    }

    private void doRefresh() {
        service.tryAcquire();
        try {
            service.refreshAll();
        } finally {
            service.release();
        }
    }
}
