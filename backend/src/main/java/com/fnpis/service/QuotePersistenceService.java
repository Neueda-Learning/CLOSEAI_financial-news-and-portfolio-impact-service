package com.fnpis.service;

import com.fnpis.domain.PricePoint;
import com.fnpis.domain.PriceQuote;
import com.fnpis.integration.QuoteSnapshot;
import com.fnpis.repository.PricePointRepository;
import com.fnpis.repository.PriceQuoteRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists one quote refresh for one symbol.
 *
 * <p>Separated from {@link QuoteRefreshService} so {@code @Transactional}
 * actually takes effect (Spring AOP proxies only intercept cross-bean calls).
 * One call = one transaction. A failure here does not roll back other symbols.
 */
@Service
public class QuotePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(QuotePersistenceService.class);
    private final PriceQuoteRepository quoteRepo;
    private final PricePointRepository pointRepo;

    public QuotePersistenceService(PriceQuoteRepository quoteRepo, PricePointRepository pointRepo) {
        this.quoteRepo = quoteRepo;
        this.pointRepo = pointRepo;
    }

    @Transactional
    public boolean persist(String symbol, QuoteSnapshot s) {
        PriceQuote q = quoteRepo.findById(symbol)
                .orElseGet(PriceQuote::new);
        q.setPrice(s.price());
        if (s.previousClose() != null) {
            q.setPreviousClose(s.previousClose());
        }
        q.setAsOf(s.capturedAt() != null ? s.capturedAt() : Instant.now());
        quoteRepo.save(q);

        Instant capturedAt = s.capturedAt() != null ? s.capturedAt() : Instant.now();
        PricePoint point = new PricePoint(symbol, capturedAt, s.price());
        pointRepo.save(point);
        return true;
    }
}
