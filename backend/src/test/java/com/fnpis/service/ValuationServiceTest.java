package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.api.internal.dto.HoldingRow;
import com.fnpis.domain.Holding;
import com.fnpis.domain.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The numbers a lecturer will check by hand (SC-002, SC-003).
 *
 * <p>Values are chosen so every expected figure is verifiable mentally - if a
 * test here fails, the arithmetic is wrong, not the fixture.
 */
class ValuationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-27T15:42:10Z");
    private final ValuationService service = new ValuationService(300);

    @Test
    @DisplayName("SC-002: totals are the exact sum of the displayed rows")
    void totalsMatchRows() {
        Valuation v = service.value(
                List.of(holding(1L, "AAPL", "50", "180.00"), holding(2L, "NVDA", "100", "140.00")),
                Map.of("AAPL", quote("AAPL", "200.00", "198.00", NOW),
                        "NVDA", quote("NVDA", "125.60", "121.40", NOW)),
                Map.of("AAPL", "Apple Inc.", "NVDA", "NVIDIA Corporation"),
                NOW);

        // 50 * 200 = 10000, 100 * 125.60 = 12560
        assertThat(v.totalMarketValue()).isEqualByComparingTo("22560.00");
        // 50 * 180 = 9000, 100 * 140 = 14000
        assertThat(v.totalCost()).isEqualByComparingTo("23000.00");
        assertThat(v.unrealizedPnL()).isEqualByComparingTo("-440.00");

        BigDecimal rowSum = v.rows().stream()
                .map(HoldingRow::marketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(rowSum).isEqualByComparingTo(v.totalMarketValue());
    }

    @Test
    @DisplayName("SC-003: weights sum to 1.0 within the allowed rounding error")
    void weightsSumToOne() {
        // Thirds do not divide evenly, which is where a naive rounding drifts.
        Valuation v = service.value(
                List.of(holding(1L, "AAPL", "1", "1.00"),
                        holding(2L, "NVDA", "1", "1.00"),
                        holding(3L, "MSFT", "1", "1.00")),
                Map.of("AAPL", quote("AAPL", "10.00", null, NOW),
                        "NVDA", quote("NVDA", "10.00", null, NOW),
                        "MSFT", quote("MSFT", "10.00", null, NOW)),
                Map.of(),
                NOW);

        double sum = v.rows().stream().mapToDouble(HoldingRow::weight).sum();
        assertThat(sum).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("EC-07: zero cost basis yields a null return percentage, not Infinity")
    void zeroCostGivesNullPct() {
        Valuation v = service.value(
                List.of(holding(1L, "MSFT", "10", "0.00")),
                Map.of("MSFT", quote("MSFT", "50.00", null, NOW)),
                Map.of(),
                NOW);

        HoldingRow row = v.rows().get(0);
        assertThat(row.marketValue()).isEqualByComparingTo("500.00");
        assertThat(row.unrealizedPnLPct()).isNull();
    }

    @Test
    @DisplayName("EC-13: a stale quote still prices the row, but flags itself unavailable")
    void staleQuoteKeepsLastKnownPrice() {
        Valuation v = service.value(
                List.of(holding(1L, "NVDA", "10", "100.00")),
                Map.of("NVDA", quote("NVDA", "125.60", null, NOW.minusSeconds(600))),
                Map.of(),
                NOW);

        HoldingRow row = v.rows().get(0);
        assertThat(row.quoteAvailable()).isFalse();
        assertThat(row.currentPrice()).isEqualByComparingTo("125.60");
        assertThat(row.marketValue()).isEqualByComparingTo("1256.00");
        assertThat(v.stale()).isTrue();
    }

    @Test
    @DisplayName("EC-13: no quote row at all leaves price and value null rather than guessing")
    void missingQuoteLeavesValueNull() {
        Valuation v = service.value(
                List.of(holding(1L, "NVDA", "10", "100.00")),
                Map.of(),
                Map.of(),
                NOW);

        HoldingRow row = v.rows().get(0);
        assertThat(row.quoteAvailable()).isFalse();
        assertThat(row.currentPrice()).isNull();
        assertThat(row.marketValue()).isNull();
        // Cost is known even when the market value is not.
        assertThat(row.totalCost()).isEqualByComparingTo("1000.00");
        assertThat(v.stale()).isTrue();
    }

    @Test
    @DisplayName("EC-01: an empty portfolio totals zero and is not stale")
    void emptyPortfolioIsNotStale() {
        Valuation v = service.value(List.of(), Map.of(), Map.of(), NOW);

        assertThat(v.totalMarketValue()).isEqualByComparingTo("0.00");
        assertThat(v.allocations()).isEmpty();
        // Nothing is out of date when nothing is held - a badge here would be noise.
        assertThat(v.stale()).isFalse();
        assertThat(v.asOf()).isNull();
    }

    @Test
    @DisplayName("Quantities go out as whole shares, not 20.0000")
    void quantityHasNoTrailingZeros() {
        Valuation v = service.value(
                List.of(holding(1L, "AAPL", "20.0000", "10.00")),
                Map.of("AAPL", quote("AAPL", "10.00", null, NOW)),
                Map.of(),
                NOW);

        assertThat(v.rows().get(0).quantity().toPlainString()).isEqualTo("20");
    }

    private static Holding holding(Long id, String symbol, String qty, String cost) {
        Holding h = new Holding(1L, symbol, new BigDecimal(qty), new BigDecimal(cost));
        h.setId(id);
        return h;
    }

    private static PriceQuote quote(String symbol, String price, String prevClose, Instant asOf) {
        return new PriceQuote(
                symbol,
                new BigDecimal(price),
                prevClose == null ? null : new BigDecimal(prevClose),
                asOf);
    }
}
