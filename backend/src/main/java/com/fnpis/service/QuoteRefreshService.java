package com.fnpis.service;

import com.fnpis.domain.PricePoint;
import com.fnpis.domain.PriceQuote;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import com.fnpis.repository.PricePointRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SecurityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuoteRefreshService {

    private static final Logger log = LoggerFactory.getLogger(QuoteRefreshService.class);
    private final PriceProvider priceProvider;
    private final PriceQuoteRepository quoteRepo;
    private final PricePointRepository pointRepo;
    private final SecurityRepository securityRepo;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public QuoteRefreshService(
            PriceProvider priceProvider,
            PriceQuoteRepository quoteRepo,
            PricePointRepository pointRepo,
            SecurityRepository securityRepo) {
        this.priceProvider = priceProvider;
        this.quoteRepo = quoteRepo;
        this.pointRepo = pointRepo;
        this.securityRepo = securityRepo;
    }

    /**
     * Attempts to acquire the execution lock.
     *
     * @return true if the lock was acquired and the caller should proceed
     */
    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    /**
     * Releases the execution lock. Must be called in a {@code finally} block.
     */
    public void release() {
        running.set(false);
    }

    /**
     * Refreshes quotes for every symbol in the watchlist.
     *
     * <p>One symbol = one transaction. A failure on symbol N does not roll back
     * symbols 1..N-1.
     *
     * @return the number of symbols successfully refreshed
     */
    public int refreshAll() {
        List<String> symbols = securityRepo.findAll()
                .stream()
                .map(s -> s.getSymbol())
                .toList();
        int success = 0;
        for (String symbol : symbols) {
            if (refreshOne(symbol)) {
                success++;
            }
        }
        log.info("Quote refresh complete: {}/{} symbols updated", success, symbols.size());
        return success;
    }

    @Transactional
    boolean refreshOne(String symbol) {
        Optional<QuoteSnapshot> snapshot;
        try {
            snapshot = priceProvider.fetchQuote(symbol);
        } catch (Exception e) {
            log.warn("Quote refresh failed for {} — skipping", symbol, e);
            return false;
        }
        if (snapshot.isEmpty()) {
            log.debug("No quote returned for {} — halted or unknown, skipping", symbol);
            return false;
        }
        QuoteSnapshot s = snapshot.get();
        PriceQuote q = quoteRepo.findById(symbol)
                .orElse(new PriceQuote(symbol, s.price(), s.previousClose(), s.capturedAt()));
        q.setPrice(s.price());
        if (s.previousClose() != null) {
            q.setPreviousClose(s.previousClose());
        }
        q.setAsOf(s.capturedAt());
        quoteRepo.save(q);

        Instant capturedAt = s.capturedAt() != null ? s.capturedAt() : Instant.now();
        PricePoint point = new PricePoint(symbol, capturedAt, s.price());
        pointRepo.save(point);
        return true;
    }
}
