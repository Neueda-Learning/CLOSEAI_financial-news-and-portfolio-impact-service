package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.domain.Portfolio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * The nightly sweep across portfolios.
 *
 * <p>These tests exist for one reason: a snapshot missed tonight cannot be taken
 * tomorrow. Past holdings are recorded nowhere, so the sweep has to survive one
 * portfolio failing rather than abandoning the rest of the run.
 */
@ExtendWith(MockitoExtension.class)
class ValuationSnapshotServiceTest {

    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);
    private static final Instant NOW = Instant.parse("2026-07-27T20:05:00Z");

    @Mock private com.fnpis.repository.PortfolioRepository portfolios;
    @Mock private ValuationSnapshotWriter writer;

    private ValuationSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new ValuationSnapshotService(portfolios, writer);
    }

    private static Portfolio portfolio(Long id) {
        Portfolio p = new Portfolio();
        p.setId(id);
        p.setName("Portfolio " + id);
        return p;
    }

    @Test
    @DisplayName("every portfolio gets its own snapshot for the session")
    void sweepsAll() {
        when(portfolios.findAll()).thenReturn(List.of(portfolio(1L), portfolio(2L)));
        when(writer.capture(any(), eq(SESSION), eq(NOW))).thenReturn(true);

        assertThat(service.captureAll(SESSION, NOW)).isEqualTo(2);

        verify(writer).capture(1L, SESSION, NOW);
        verify(writer).capture(2L, SESSION, NOW);
    }

    @Test
    @DisplayName("one portfolio failing does not cost the others their only chance")
    void oneFailureDoesNotAbortTheRun() {
        when(portfolios.findAll())
                .thenReturn(List.of(portfolio(1L), portfolio(2L), portfolio(3L)));
        when(writer.capture(eq(1L), any(), any())).thenReturn(true);
        when(writer.capture(eq(2L), any(), any()))
                .thenThrow(new DataIntegrityViolationException("bad row"));
        when(writer.capture(eq(3L), any(), any())).thenReturn(true);

        // Two written, not zero, and not an exception out of the job: tonight's
        // close is the only time these two could ever have been recorded.
        assertThat(service.captureAll(SESSION, NOW)).isEqualTo(2);

        verify(writer).capture(3L, SESSION, NOW);
    }

    @Test
    @DisplayName("an unvaluable portfolio is not counted as written")
    void unvaluableIsNotCounted() {
        when(portfolios.findAll()).thenReturn(List.of(portfolio(1L)));
        when(writer.capture(any(), any(), any())).thenReturn(false);

        assertThat(service.captureAll(SESSION, NOW)).isZero();
    }

    @Test
    @DisplayName("no portfolios is a no-op, not a failure")
    void noPortfolios() {
        when(portfolios.findAll()).thenReturn(List.of());

        assertThat(service.captureAll(SESSION, NOW)).isZero();
    }
}
