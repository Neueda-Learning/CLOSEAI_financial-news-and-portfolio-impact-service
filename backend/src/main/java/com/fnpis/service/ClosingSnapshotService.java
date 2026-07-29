package com.fnpis.service;

import com.fnpis.domain.PriceBar;
import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.repository.PriceBarRepository;
import com.fnpis.repository.SecurityRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Captures daily bars for every symbol in the watchlist.
 *
 * <p>Intended to fire shortly after the US market closes (16:05 ET).
 * Uses the write chain (Finnhub → Twelve Data) — no DB fallback for the
 * same reason as {@link QuoteRefreshService}: a dead upstream must not
 * silently log 15/15 success.
 *
 * <p>One symbol = one transaction.
 */
@Service
public class ClosingSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ClosingSnapshotService.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private final PriceProvider priceProvider;
    private final PriceBarRepository barRepo;
    private final SecurityRepository securityRepo;
    private final Clock clock;

    public ClosingSnapshotService(
            @Qualifier("writePriceProvider") PriceProvider priceProvider,
            PriceBarRepository barRepo,
            SecurityRepository securityRepo,
            Clock clock) {
        this.priceProvider = priceProvider;
        this.barRepo = barRepo;
        this.securityRepo = securityRepo;
        this.clock = clock;
    }

    /**
     * Captures bars for the current ET session date.
     *
     * @return number of symbols successfully captured
     */
    public int capture() {
        List<String> symbols = securityRepo.findAll()
                .stream()
                .map(s -> s.getSymbol())
                .toList();
        LocalDate sessionDate = LocalDate.ofInstant(Instant.now(clock), MARKET_ZONE);
        int success = 0;
        for (String symbol : symbols) {
            try {
                List<DailyBar> bars = priceProvider.fetchDailyBars(symbol, sessionDate, sessionDate);
                if (bars.isEmpty()) {
                    log.debug("No bar for {} on {}", symbol, sessionDate);
                    continue;
                }
                DailyBar b = bars.get(0);
                barRepo.save(new PriceBar(b.symbol(), b.date(), b.close()));
                success++;
            } catch (Exception e) {
                log.warn("Closing snapshot failed for {} — skipping", symbol, e.getClass().getSimpleName());
            }
        }
        log.info("Closing snapshot complete: {}/{} symbols captured", success, symbols.size());
        return success;
    }
}
