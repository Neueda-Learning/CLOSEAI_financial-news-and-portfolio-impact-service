package com.fnpis.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Fetch price data from an external source (Finnhub, Twelve Data, local DB,
 * or a mock for E2E tests).
 *
 * <p>Services depend on this interface, never on an implementation — that is
 * what makes swapping vendors a configuration change, not a code change.
 */
public interface PriceProvider {

    /**
     * Latest quote for one symbol.
     *
     * @return empty when the symbol is halted or unknown (EC-13); the caller
     *         must not insert a zero or null price into the database
     */
    Optional<QuoteSnapshot> fetchQuote(String symbol);

    /**
     * Daily OHLC bars for a date range.
     *
     * <p>Belongs to Module B5 (closing snapshot). Providers that do not yet
     * implement this throw {@link UnsupportedOperationException}. When B5
     * lands, each provider replaces the throw with the real implementation
     * — the interface itself does not change.
     */
    default List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "fetchDailyBars is not implemented yet — see Module B5");
    }
}
