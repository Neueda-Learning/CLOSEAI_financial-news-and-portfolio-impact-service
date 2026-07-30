package com.fnpis.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Decides which trading session a news story is charged to
 * (requirements 5.3 step 1, EC-16, EC-17).
 *
 * <p>A story published after the close, or on a weekend, belongs to the next
 * session - the market had no chance to react before then, so charging it to the
 * day it was published would compare it against a price move that happened
 * before it was news.
 *
 * <p><b>No US holiday calendar</b>, matching the convention module B settled on
 * for quote refresh (module B spec section 6). The cost is that a story
 * published on Thanksgiving is charged to Thanksgiving, where {@code price_bar}
 * has no row, so no return can be computed and the assessment is INCONCLUSIVE.
 * That is a "cannot tell" rather than a wrong number, which is why it is
 * acceptable. It is a known deviation, not a bug to fix.
 */
@Component
public class AttributionDateResolver {

    /**
     * US market timezone. Named rather than the server default so the answer is
     * the same in CI, on a laptop, and in a container - and so daylight saving is
     * handled by the timezone database instead of by hand.
     */
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    /** Regular session close, 16:00 US/Eastern (module B spec section 6). */
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    /**
     * Resolves the session a story is attributed to.
     *
     * @param publishedAt publication time in UTC, as stored on
     *                    {@code news_article.published_at}
     * @return the trading session the story is charged to
     * @throws NullPointerException if {@code publishedAt} is null - a story with
     *                              no timestamp is dropped at ingestion (EC-14),
     *                              so reaching here means an upstream bug
     */
    public LocalDate resolve(Instant publishedAt) {
        // Convert before comparing. Judging "after the close" against the server's
        // local clock gives different answers in CI and on a laptop, and gets the
        // attribution date wrong for every story published in the evening.
        ZonedDateTime marketTime = publishedAt.atZone(MARKET_ZONE);

        LocalDate candidate = marketTime.toLocalDate();
        if (!marketTime.toLocalTime().isBefore(MARKET_CLOSE)) {
            candidate = candidate.plusDays(1);
        }

        return nextTradingDay(candidate);
    }

    /** Walks forward off Saturday and Sunday (EC-17). */
    private LocalDate nextTradingDay(LocalDate date) {
        LocalDate result = date;
        while (isWeekend(result)) {
            result = result.plusDays(1);
        }
        return result;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}