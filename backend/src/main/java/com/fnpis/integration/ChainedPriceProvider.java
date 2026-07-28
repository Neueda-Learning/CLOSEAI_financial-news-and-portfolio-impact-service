package com.fnpis.integration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Tries providers in chain: Finnhub → Twelve Data → local DB.
 *
 * <p>Each upstream failure falls through to the next provider. The DB provider
 * is the ultimate fallback. The service layer sees only this bean via the
 * {@link PriceProvider} interface.
 */
@Component
@Primary
public class ChainedPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(ChainedPriceProvider.class);
    private final PriceProvider finnhub;
    private final PriceProvider twelvedata;
    private final PriceProvider db;

    public ChainedPriceProvider(
            @Qualifier("finnhubPriceProvider") PriceProvider finnhub,
            @Qualifier("twelveDataPriceProvider") PriceProvider twelvedata,
            @Qualifier("dbPriceProvider") PriceProvider db) {
        this.finnhub = finnhub;
        this.twelvedata = twelvedata;
        this.db = db;
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
            Optional<QuoteSnapshot> result = twelvedata.fetchQuote(symbol);
            if (result.isPresent()) {
                return result;
            }
        } catch (Exception e2) {
            log.warn("Twelve Data failed for {}, falling back to DB cache", symbol, e2);
        }
        return db.fetchQuote(symbol);
    }

    @Override
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "Daily bars not implemented yet — see Module B5");
    }
}
