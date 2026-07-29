package com.fnpis.service;

import com.fnpis.integration.NewsItem;
import com.fnpis.integration.NewsProvider;
import com.fnpis.repository.SecurityRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NewsFetchService {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchService.class);
    private final NewsProvider newsProvider;
    private final NewsPersistenceService persistence;
    private final SecurityRepository securityRepo;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NewsFetchService(
            NewsProvider newsProvider,
            NewsPersistenceService persistence,
            SecurityRepository securityRepo) {
        this.newsProvider = newsProvider;
        this.persistence = persistence;
        this.securityRepo = securityRepo;
    }

    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    public void release() {
        running.set(false);
    }

    /** Result of one fetch cycle: how many new links were created vs skipped. */
    public record FetchResult(int inserted, int skippedDuplicates) {}

    /**
     * Fetches news for every symbol in the watchlist.
     * One symbol = one transaction — delegated to {@link NewsPersistenceService}
     * so {@code @Transactional} takes effect via Spring AOP.
     */
    public FetchResult fetchAll() {
        List<String> symbols = securityRepo.findAll()
                .stream()
                .map(s -> s.getSymbol())
                .toList();
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        int inserted = 0;
        int skipped = 0;
        for (String symbol : symbols) {
            int result = fetchForSymbol(symbol, today.minusDays(7), today);
            if (result > 0) {
                inserted += result;
            } else {
                skipped++;
            }
        }
        log.info("News fetch complete: {} inserted, {} symbols all-duplicate",
                inserted, skipped);
        return new FetchResult(inserted, skipped);
    }

    private int fetchForSymbol(String symbol, LocalDate from, LocalDate to) {
        List<NewsItem> items;
        try {
            items = newsProvider.fetchCompanyNews(symbol, from, to);
        } catch (Exception e) {
            log.warn("News fetch failed for {} — skipping", symbol, e);
            return 0;
        }
        int count = 0;
        for (NewsItem item : items) {
            count += persistence.persist(symbol, item);
        }
        return count;
    }
}
