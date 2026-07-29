package com.fnpis.service;

import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import com.fnpis.repository.SecurityRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuoteRefreshService {

    private static final Logger log = LoggerFactory.getLogger(QuoteRefreshService.class);
    private final PriceProvider priceProvider;
    private final QuotePersistenceService persistence;
    private final SecurityRepository securityRepo;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public QuoteRefreshService(
            @org.springframework.beans.factory.annotation.Qualifier("writePriceProvider") PriceProvider priceProvider,
            QuotePersistenceService persistence,
            SecurityRepository securityRepo) {
        this.priceProvider = priceProvider;
        this.persistence = persistence;
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
     * <p>One symbol = one transaction — delegated to {@link QuotePersistenceService}
     * so {@code @Transactional} takes effect via Spring AOP.
     * A failure on symbol N does not roll back symbols 1..N-1.
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
            if (persistOne(symbol)) {
                success++;
            }
        }
        log.info("Quote refresh complete: {}/{} symbols updated", success, symbols.size());
        return success;
    }

    private boolean persistOne(String symbol) {
        Optional<QuoteSnapshot> snapshot;
        try {
            snapshot = priceProvider.fetchQuote(symbol);
        } catch (Exception e) {
            log.warn("Quote refresh failed for {} — skipping", symbol, e.getClass().getSimpleName());
            return false;
        }
        if (snapshot.isEmpty()) {
            log.debug("No quote returned for {} — halted or unknown, skipping", symbol);
            return false;
        }
        try {
            return persistence.persist(symbol, snapshot.get());
        } catch (Exception e) {
            log.warn("Quote persistence failed for {} — skipping", symbol, e.getClass().getSimpleName());
            return false;
        }
    }
}
