package com.fnpis.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.service.ImpactAssessmentService;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Which recompute a trigger reaches, and what a busy service answers.
 *
 * <p>The lock behaviour matters more here than for the news poll: a recompute
 * overwrites a whole session's rows, so a second concurrent run does not just
 * waste work, it rewrites numbers a reader may already be looking at.
 */
@ExtendWith(MockitoExtension.class)
class ImpactRecomputeSchedulerTest {

    private static final Long PORTFOLIO_ID = 7L;
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);

    @Mock private ImpactAssessmentService service;

    private ImpactRecomputeScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ImpactRecomputeScheduler(service);
    }

    private void lockIsFree() {
        when(service.tryAcquire()).thenReturn(true);
    }

    @Nested
    @DisplayName("the manual trigger picks a scope from the body")
    class Scope {

        @Test
        @DisplayName("no portfolio id recomputes every portfolio")
        void nullIdMeansAll() {
            lockIsFree();
            when(service.recomputeFor(eq(SESSION), any(Instant.class))).thenReturn(12);

            assertThat(scheduler.manualRecompute(null, SESSION)).isEqualTo(12);

            verify(service, never()).recomputeOne(anyLong(), any(), any());
        }

        @Test
        @DisplayName("a portfolio id recomputes only that one")
        void idMeansOne() {
            lockIsFree();
            when(service.recomputeOne(eq(PORTFOLIO_ID), eq(SESSION), any(Instant.class)))
                    .thenReturn(3);

            assertThat(scheduler.manualRecompute(PORTFOLIO_ID, SESSION)).isEqualTo(3);

            // The all-portfolios path must stay out of reach: it would silently
            // rewrite sessions the caller did not ask about.
            verify(service, never()).recomputeFor(any(LocalDate.class), any());
        }
    }

    @Nested
    @DisplayName("a run already in flight")
    class AlreadyRunning {

        @Test
        @DisplayName("EC-20: the manual trigger is refused with 409, not queued")
        void manualRefuses() {
            when(service.tryAcquire()).thenReturn(false);

            assertThatThrownBy(() -> scheduler.manualRecompute(null, SESSION))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.TASK_ALREADY_RUNNING);

            verify(service, never()).recomputeFor(any(LocalDate.class), any());
            // Nothing was claimed, so nothing may be released - releasing here
            // would unlock the run that is genuinely in progress.
            verify(service, never()).release();
        }

        @Test
        @DisplayName("the timer skips quietly rather than throwing")
        void scheduledSkips() {
            when(service.tryAcquire()).thenReturn(false);

            scheduler.scheduledRecompute();

            verify(service, never()).recomputeFor(any(Instant.class));
            verify(service, never()).release();
        }
    }

    @Nested
    @DisplayName("the lock is always given back")
    class LockRelease {

        @Test
        @DisplayName("a failing run still releases")
        void releasesOnFailure() {
            lockIsFree();
            when(service.recomputeFor(eq(SESSION), any(Instant.class)))
                    .thenThrow(new IllegalStateException("database went away"));

            assertThatThrownBy(() -> scheduler.manualRecompute(null, SESSION))
                    .isInstanceOf(IllegalStateException.class);

            // Without the finally, one failure would wedge every later run into a
            // permanent 409.
            verify(service).release();
        }

        @Test
        @DisplayName("a failing timer run still releases")
        void timerReleasesOnFailure() {
            lockIsFree();
            when(service.recomputeFor(any(Instant.class)))
                    .thenThrow(new IllegalStateException("provider blew up"));

            assertThatThrownBy(() -> scheduler.scheduledRecompute())
                    .isInstanceOf(IllegalStateException.class);

            verify(service).release();
        }
    }
}
