package com.fnpis.service;

import com.fnpis.domain.Holding;
import java.math.BigDecimal;

/**
 * One position with its money figures resolved, before weights are known.
 *
 * <p>Intermediate result of {@link ValuationService}'s first pass. Weight needs
 * the portfolio total, which is only available once every position has been
 * priced, so it is filled in later.
 *
 * @param currentPrice  null when no quote row exists for the symbol (EC-13)
 * @param marketValue   null for the same reason - deliberately not falling back
 *                      to cost, which would look like a real valuation
 * @param dayChange     null when the quote has no previous close (EC-19)
 * @param quoteFresh    true only when a quote exists and is inside its freshness
 *                      budget; drives {@code quoteAvailable} on the wire
 */
record Priced(
        Holding holding,
        String companyName,
        BigDecimal totalCost,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal marketValue,
        BigDecimal dayChange,
        boolean quoteFresh) {

    /** Gain or loss against cost, null when there is no market value to compare. */
    BigDecimal unrealizedPnL() {
        return marketValue == null ? null : marketValue.subtract(totalCost);
    }
}
