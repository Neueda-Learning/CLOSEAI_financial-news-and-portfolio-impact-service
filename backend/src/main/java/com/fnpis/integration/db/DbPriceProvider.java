package com.fnpis.integration.db;

import com.fnpis.domain.PriceQuote;
import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import com.fnpis.repository.PriceQuoteRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Ultimate fallback: reads the last known quote from the database.
 *
 * <p>This provider never makes an HTTP call — it returns whatever was last written
 * by a successful Finnhub or Twelve Data refresh. Used when both upstream providers
 * are unavailable.
 */
@Component
@Qualifier("dbPriceProvider")
class DbPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(DbPriceProvider.class);
    private final PriceQuoteRepository repo;

    DbPriceProvider(PriceQuoteRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<QuoteSnapshot> fetchQuote(String symbol) {
        Optional<PriceQuote> existing = repo.findById(symbol);
        if (existing.isEmpty()) {
            log.debug("No cached quote for {}", symbol);
            return Optional.empty();
        }
        PriceQuote q = existing.get();
        log.debug("Serving cached quote for {} from {} (stale)", symbol, q.getAsOf());
        return Optional.of(new QuoteSnapshot(
                q.getSymbol(), q.getPrice(), q.getPreviousClose(), q.getAsOf()));
    }

    @Override
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "DB daily bars not implemented yet — see Module B5");
    }
}
