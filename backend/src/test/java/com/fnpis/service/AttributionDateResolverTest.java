package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Attribution date cases (EC-16, EC-17).
 *
 * <p>Every input is an explicit {@link Instant}. The resolver never calls
 * {@code Instant.now()}, which is what makes the weekend and after-hours cases
 * testable at all.
 */
class AttributionDateResolverTest {

    private final AttributionDateResolver resolver = new AttributionDateResolver();

    @Test
    @DisplayName("a story published intraday is charged to that session")
    void intradayStaysOnTheSameDay() {
        // 2026-07-27 is a Monday. 12:31Z is 08:31 in New York - before the open,
        // but still that session: the market gets its chance to react that day.
        assertThat(resolver.resolve(Instant.parse("2026-07-27T12:31:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @DisplayName("a story published after the close rolls to the next session")
    void afterCloseRollsForward() {
        // 21:30Z is 17:30 in New York, past the 16:00 close (EC-16).
        assertThat(resolver.resolve(Instant.parse("2026-07-27T21:30:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 28));
    }

    @Test
    @DisplayName("the close itself counts as after hours")
    void exactlyAtCloseRollsForward() {
        // 20:00Z is exactly 16:00 in New York. The session is over at the bell, so
        // the boundary belongs to the next day - flipping this comparison would
        // charge the story to a session that had already ended.
        assertThat(resolver.resolve(Instant.parse("2026-07-27T20:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 28));
    }

    @Test
    @DisplayName("one minute before the close still counts as that session")
    void justBeforeCloseStaysOnTheSameDay() {
        assertThat(resolver.resolve(Instant.parse("2026-07-27T19:59:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @DisplayName("a Saturday story is charged to the following Monday")
    void saturdayRollsToMonday() {
        // 2026-07-25 is a Saturday (EC-17).
        assertThat(resolver.resolve(Instant.parse("2026-07-25T14:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @DisplayName("a Sunday story is charged to the following Monday")
    void sundayRollsToMonday() {
        assertThat(resolver.resolve(Instant.parse("2026-07-26T14:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @DisplayName("a Friday evening story skips the weekend entirely")
    void fridayAfterCloseRollsToMonday() {
        // 2026-07-24 is a Friday. After the close, so it rolls to Saturday, then
        // has to keep walking to Monday - the two rules compose.
        assertThat(resolver.resolve(Instant.parse("2026-07-24T22:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    @DisplayName("late UTC evening is still the same New York session")
    void utcEveningIsStillTheSameMarketDay() {
        // 2026-07-28T01:00Z is 21:00 on 07-27 in New York: a different UTC date,
        // and after that session's close. Judging this in UTC would charge it to
        // 07-28 for the wrong reason and get 07-29 after the roll.
        assertThat(resolver.resolve(Instant.parse("2026-07-28T01:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 7, 28));
    }

    @Test
    @DisplayName("winter dates resolve under EST without special handling")
    void winterDateUsesEasternStandardTime() {
        // 2026-01-15 is a Thursday. 20:00Z is 15:00 EST - still inside the
        // session, because the offset is -5 in January rather than -4. A hardcoded
        // offset would push this to the next day.
        assertThat(resolver.resolve(Instant.parse("2026-01-15T20:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 1, 15));
    }
}