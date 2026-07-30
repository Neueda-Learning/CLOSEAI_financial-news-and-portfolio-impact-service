package com.fnpis.integration;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What a price provider fetched — not an {@code @Entity}.
 *
 * <p>Returned by {@link PriceProvider#fetchQuote}. The service layer converts
 * this into a {@link com.fnpis.domain.PriceQuote} before persisting. Keeping
 * the record free of JPA annotations means mock providers and test fixtures
 * never need to touch the persistence layer.
 *
 * @param previousClose null when the listing is under two sessions old (EC-19)
 */
public record QuoteSnapshot(
        String symbol,
        BigDecimal price,
        BigDecimal previousClose,
        Instant capturedAt) {
}
