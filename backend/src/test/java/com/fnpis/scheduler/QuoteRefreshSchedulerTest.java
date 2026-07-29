package com.fnpis.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.service.QuoteRefreshService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuoteRefreshSchedulerTest {

    @Mock
    QuoteRefreshService service;

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    @Test
    void mondayBeforeOpenIsFalse() {
        var t = et(2026, 7, 27, 9, 29); // Monday 09:29 ET
        assertThat(scheduler().isTradingHours(t)).isFalse();
    }

    @Test
    void mondayAtOpenIsTrue() {
        var t = et(2026, 7, 27, 9, 30); // Monday 09:30 ET
        assertThat(scheduler().isTradingHours(t)).isTrue();
    }

    @Test
    void mondayBeforeCloseIsTrue() {
        var t = et(2026, 7, 27, 15, 59); // Monday 15:59 ET
        assertThat(scheduler().isTradingHours(t)).isTrue();
    }

    @Test
    void mondayAtCloseIsFalse() {
        var t = et(2026, 7, 27, 16, 0); // Monday 16:00 ET
        assertThat(scheduler().isTradingHours(t)).isFalse();
    }

    @Test
    void saturdayIsFalse() {
        var t = et(2026, 8, 1, 12, 0); // Saturday 12:00 ET
        assertThat(scheduler().isTradingHours(t)).isFalse();
    }

    @Test
    void winterUsesEasternStandardTime() {
        // Jan 15 2026 20:00 UTC = 15:00 EST — still in hours
        var t = Instant.parse("2026-01-15T20:00:00Z");
        assertThat(scheduler().isTradingHours(t)).isTrue();
    }

    private QuoteRefreshScheduler scheduler() {
        return new QuoteRefreshScheduler(service, java.time.Clock.systemUTC());
    }

    private static Instant et(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour, minute),
                MARKET_ZONE).toInstant();
    }
}
