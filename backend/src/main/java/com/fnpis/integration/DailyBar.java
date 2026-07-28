package com.fnpis.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One session of OHLC data from a price provider — not an {@code @Entity}.
 *
 * <p>Converted into {@link com.fnpis.domain.PriceBar} by the service layer.
 * Belongs to Module B5 (closing snapshot); declared here so the interface
 * contract is settled now and B5 only needs to add implementations.
 */
public record DailyBar(
        String symbol,
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume) {
}
