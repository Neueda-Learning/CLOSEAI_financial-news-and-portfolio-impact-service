package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.domain.Holding;
import com.fnpis.domain.PriceQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The weights module E consumes (requirements 5.3).
 *
 * <p>Built on the worked example in that section so a failure here means module
 * A stopped agreeing with the document, not that a fixture drifted.
 */
class PortfolioWeightsTest {

    private static final Instant NOW = Instant.parse("2026-07-27T15:42:10Z");
    private final ValuationService service = new ValuationService(300);

    @Test
    @DisplayName("Worked example 1: NVDA weight reproduces the documented 0.303")
    void reproducesWorkedExample() {
        // 5.3 example 1: NVDA at 38900.25 in a 128450.75 portfolio. Single
        // shares priced at the documented market values, so the fixture carries
        // no rounding slack of its own - what is asserted is the weight
        // arithmetic, not a share count.
        Valuation v = service.value(
                List.of(holding("NVDA", "1", "100.00"), holding("REST", "1", "0.00")),
                Map.of("NVDA", quote("NVDA", "38900.25", NOW),
                        "REST", quote("REST", "89550.50", NOW)),
                Map.of(),
                NOW);

        PortfolioWeights weights = v.weights();
        BigDecimal nvda = weights.of("NVDA").orElseThrow().weight();

        assertThat(weights.totalMarketValue()).isEqualByComparingTo("128450.75");
        assertThat(weights.of("NVDA").orElseThrow().marketValue()).isEqualByComparingTo("38900.25");
        assertThat(nvda.setScale(3, RoundingMode.HALF_UP)).isEqualByComparingTo("0.303");
        // Full precision, for reference: 38900.25 / 128450.75.
        assertThat(nvda).isEqualByComparingTo("0.302842");
    }

    @Test
    @DisplayName("Weight is BigDecimal at 6 decimals, so module E multiplies without converting")
    void weightKeepsPrecision() {
        Valuation v = service.value(
                List.of(holding("AAPL", "1", "1.00"), holding("NVDA", "2", "1.00")),
                Map.of("AAPL", quote("AAPL", "10.00", NOW), "NVDA", quote("NVDA", "10.00", NOW)),
                Map.of(),
                NOW);

        BigDecimal weight = v.weights().of("AAPL").orElseThrow().weight();
        assertThat(weight.scale()).isEqualTo(6);
        assertThat(weight).isEqualByComparingTo("0.333333");
    }

    @Test
    @DisplayName("SC-003 holds on this path too: weights sum to 1.0")
    void weightsSumToOne() {
        Valuation v = service.value(
                List.of(holding("AAPL", "50", "180.00"), holding("NVDA", "100", "140.00")),
                Map.of("AAPL", quote("AAPL", "200.00", NOW), "NVDA", quote("NVDA", "125.60", NOW)),
                Map.of(),
                NOW);

        BigDecimal sum = v.weights().bySymbol().values().stream()
                .map(PositionWeight::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("1.000000");
    }

    @Test
    @DisplayName("A position with no quote is kept but marked unassessable, not dropped")
    void unquotedPositionIsKeptAndFlagged() {
        Valuation v = service.value(
                List.of(holding("AAPL", "10", "10.00"), holding("MSFT", "10", "10.00")),
                Map.of("AAPL", quote("AAPL", "10.00", NOW)),
                Map.of(),
                NOW);

        PortfolioWeights weights = v.weights();
        // Present, so module E can tell "held but unpriceable" from "not held".
        assertThat(weights.bySymbol()).containsKeys("AAPL", "MSFT");

        PositionWeight msft = weights.of("MSFT").orElseThrow();
        assertThat(msft.assessable()).isFalse();
        assertThat(msft.weight()).isNull();
        assertThat(msft.marketValue()).isNull();
        // Quantity is known even when the price is not.
        assertThat(msft.quantity()).isEqualByComparingTo("10");

        assertThat(weights.of("AAPL").orElseThrow().assessable()).isTrue();
        assertThat(weights.isAssessable()).isTrue();
    }

    @Test
    @DisplayName("EC-01: an empty portfolio yields nothing to assess rather than a zero weight")
    void emptyPortfolioIsNotAssessable() {
        PortfolioWeights weights = service.value(List.of(), Map.of(), Map.of(), NOW).weights();

        assertThat(weights.bySymbol()).isEmpty();
        assertThat(weights.isAssessable()).isFalse();
        assertThat(weights.of("NVDA")).isEmpty();
        assertThat(weights.totalMarketValue()).isEqualByComparingTo("0.00");
    }

    private static Holding holding(String symbol, String qty, String cost) {
        return new Holding(1L, symbol, new BigDecimal(qty), new BigDecimal(cost));
    }

    private static PriceQuote quote(String symbol, String price, Instant asOf) {
        return new PriceQuote(symbol, new BigDecimal(price), null, asOf);
    }
}
