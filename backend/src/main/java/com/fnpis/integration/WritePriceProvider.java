package com.fnpis.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Write-path price chain: Finnhub → Twelve Data. No DB fallback.
 *
 * <p>The DB provider serves read endpoints (decision 2: stale data is better
 * than an error). The write path must not reuse it — otherwise a completely
 * dead upstream logs 15/15 success while achieving zero new data.
 *
 * <p>Marked {@code @Primary} so the existing {@code @Primary} on
 * {@link ChainedPriceProvider} remains the only one — services inject the
 * write chain via {@code @Qualifier("writePriceProvider")}.
 */
@Component
@Qualifier("writePriceProvider")
@ConditionalOnProperty(name = "app.providers.price", havingValue = "finnhub", matchIfMissing = true)
public class WritePriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(WritePriceProvider.class);
    private final PriceProvider finnhub;
    private final PriceProvider twelvedata;

    public WritePriceProvider(
            @Qualifier("finnhubPriceProvider") PriceProvider finnhub,
            @Qualifier("twelveDataPriceProvider") PriceProvider twelvedata) {
        this.finnhub = finnhub;
        this.twelvedata = twelvedata;
    }

    @Override
    public Optional<QuoteSnapshot> fetchQuote(String symbol) {
        try {
            Optional<QuoteSnapshot> result = finnhub.fetchQuote(symbol);
            if (result.isPresent()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Finnhub failed for {}, trying Twelve Data", symbol, e);
        }
        try {
            return twelvedata.fetchQuote(symbol);
        } catch (Exception e2) {
            log.warn("Twelve Data failed for {} — skipping", symbol, e2);
        }
        return Optional.empty();
    }

    @Override
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "Daily bars not implemented yet — see Module B5");
    }
}
