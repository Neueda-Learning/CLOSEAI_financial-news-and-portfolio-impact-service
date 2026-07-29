package com.fnpis.service;

import com.fnpis.domain.PriceBar;
import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.repository.PriceBarRepository;
import com.fnpis.repository.SecurityRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Captures the last trading day's OHLC bars for every symbol in the watchlist.
 *
 * <p>Uses the write chain (Finnhub → Twelve Data) — no DB fallback for the
 * same reason as {@link QuoteRefreshService}: a dead upstream must not
 * silently log 15/15 success.
 *
 * <p>One symbol = one transaction.
 */
@Service
public class ClosingSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ClosingSnapshotService.class);
    private final PriceProvider priceProvider;
    private final PriceBarRepository barRepo;
    private final SecurityRepository securityRepo;

    public ClosingSnapshotService(
            @Qualifier("writePriceProvider") PriceProvider priceProvider,
            PriceBarRepository barRepo,
            SecurityRepository securityRepo) {
        this.priceProvider = priceProvider;
        this.barRepo = barRepo;
        this.securityRepo = securityRepo;
    }

    /**
     * Captures bars for yesterday (the last completed trading day).
     *
     * @return number of symbols successfully captured
     */
    public int capture() {
        List<String> symbols = securityRepo.findAll()
                .stream()
                .map(s -> s.getSymbol())
                .toList();
        LocalDate today = LocalDate.now();
        int success = 0;
        for (String symbol : symbols) {
            try {
                List<DailyBar> bars = priceProvider.fetchDailyBars(symbol, today, today);
                if (bars.isEmpty()) {
                    log.debug("No bar for {} on {}", symbol, today);
                    continue;
                }
                DailyBar b = bars.get(0);
                barRepo.save(new PriceBar(b.symbol(), b.date(), b.close()));
                success++;
            } catch (Exception e) {
                log.warn("Closing snapshot failed for {} — skipping", symbol, e);
            }
        }
        log.info("Closing snapshot complete: {}/{} symbols captured", success, symbols.size());
        return success;
    }
}
