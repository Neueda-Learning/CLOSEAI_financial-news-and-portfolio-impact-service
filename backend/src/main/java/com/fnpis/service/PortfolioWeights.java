package com.fnpis.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Every position's weight in one portfolio, keyed by symbol.
 *
 * <p>Built for the access pattern module E actually has: a news article names a
 * symbol, and the engine needs that symbol's weight and market value. A list
 * would make every lookup a scan.
 *
 * @param bySymbol         one entry per position held, including positions with
 *                         no quote - E needs to know they exist even when it
 *                         cannot assess them
 * @param totalMarketValue the denominator behind every weight, {@code 0.00} for
 *                         an empty portfolio
 * @param asOf             oldest quote behind these figures, null when none exist
 * @param stale            true when any quote is past its budget or missing;
 *                         an impact computed off stale prices is still worth
 *                         recording, but should say so (B4, SC-009)
 */
public record PortfolioWeights(
        Map<String, PositionWeight> bySymbol,
        BigDecimal totalMarketValue,
        Instant asOf,
        boolean stale) {

    /** The weight of one symbol, empty when the portfolio does not hold it. */
    public Optional<PositionWeight> of(String symbol) {
        return Optional.ofNullable(bySymbol.get(symbol));
    }

    /** True when at least one position can be assessed. False for an empty portfolio, or one with no quotes at all. */
    public boolean isAssessable() {
        return bySymbol.values().stream().anyMatch(PositionWeight::assessable);
    }
}
