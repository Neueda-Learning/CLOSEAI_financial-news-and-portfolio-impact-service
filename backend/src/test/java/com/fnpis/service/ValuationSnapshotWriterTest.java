package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.domain.Holding;
import com.fnpis.domain.PortfolioValuationSnapshot;
import com.fnpis.domain.PriceQuote;
import com.fnpis.repository.PortfolioValuationSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * One portfolio's closing snapshot.
 *
 * <p>The row this writes is the only record of what a portfolio was worth on a
 * given day - F5's chart has no other source, and the value cannot be recomputed
 * later because past holdings are not stored. So the assertions here are about
 * the row being written at all, and carrying the total the screens would show.
 */
@ExtendWith(MockitoExtension.class)
class ValuationSnapshotWriterTest {

    private static final Long PORTFOLIO_ID = 1L;
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);
    private static final Instant NOW = Instant.parse("2026-07-27T20:05:00Z");

    @Mock private PortfolioValuationSnapshotRepository snapshots;
    @Mock private HoldingValuationLoader loader;

    private ValuationSnapshotWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ValuationSnapshotWriter(snapshots, loader);
    }

    /** A real Valuation, not a mock: the total has to be the one the screens show. */
    private static Valuation valuationOf(String qty, String price) {
        Holding holding = new Holding(
                PORTFOLIO_ID, "NVDA", new BigDecimal(qty), new BigDecimal("100.00"));
        PriceQuote quote = new PriceQuote(
                "NVDA", new BigDecimal(price), new BigDecimal("121.40"), NOW);
        return new ValuationService(300).value(
                List.of(holding), Map.of("NVDA", quote), Map.of("NVDA", "NVIDIA"), NOW);
    }

    private PortfolioValuationSnapshot written() {
        ArgumentCaptor<PortfolioValuationSnapshot> saved =
                ArgumentCaptor.forClass(PortfolioValuationSnapshot.class);
        verify(snapshots).save(saved.capture());
        return saved.getValue();
    }

    @Test
    @DisplayName("the stored total is the summed market value, keyed by portfolio and session")
    void storesTheTotal() {
        when(loader.value(PORTFOLIO_ID, NOW)).thenReturn(valuationOf("10", "126.44"));

        assertThat(writer.capture(PORTFOLIO_ID, SESSION, NOW)).isTrue();

        PortfolioValuationSnapshot row = written();
        assertThat(row.getPortfolioId()).isEqualTo(PORTFOLIO_ID);
        assertThat(row.getSnapshotDate()).isEqualTo(SESSION);
        // 10 x 126.44. Compared by value: the scale is the column's, not the test's.
        assertThat(row.getTotalValue()).isEqualByComparingTo("1264.40");
    }

    @Test
    @DisplayName("EC-01: an empty portfolio is worth zero, and zero is worth recording")
    void emptyPortfolioStillGetsARow() {
        Valuation empty = new ValuationService(300).value(List.of(), Map.of(), Map.of(), NOW);
        when(loader.value(PORTFOLIO_ID, NOW)).thenReturn(empty);

        assertThat(writer.capture(PORTFOLIO_ID, SESSION, NOW)).isTrue();

        // A skipped row would leave a gap in the line that reads as a failed job
        // rather than as an empty portfolio.
        assertThat(written().getTotalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("EC-13: a position with no quote contributes nothing but does not block the row")
    void unquotedPositionStillSnapshots() {
        Holding held = new Holding(
                PORTFOLIO_ID, "NVDA", new BigDecimal("10"), new BigDecimal("100.00"));
        Valuation unquoted = new ValuationService(300)
                .value(List.of(held), Map.of(), Map.of(), NOW);
        when(loader.value(PORTFOLIO_ID, NOW)).thenReturn(unquoted);

        assertThat(writer.capture(PORTFOLIO_ID, SESSION, NOW)).isTrue();

        // Zero rather than no row: the alternative is losing the session for every
        // other position in the portfolio because one symbol had no quote.
        assertThat(written().getTotalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a portfolio that cannot be valued at all writes nothing")
    void nullTotalWritesNothing() {
        Valuation broken = new Valuation(List.of(), null, BigDecimal.ZERO, null, NOW, true);
        when(loader.value(PORTFOLIO_ID, NOW)).thenReturn(broken);

        assertThat(writer.capture(PORTFOLIO_ID, SESSION, NOW)).isFalse();

        // Better an absent point than a null one the chart has to guess about.
        verify(snapshots, never()).save(any());
    }
}
