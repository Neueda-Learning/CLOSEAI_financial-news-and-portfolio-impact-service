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

    /** Result of one fetch cycle. */
    public record FetchResult(boolean triggered, int fetched, int inserted,
            int skippedDuplicates) {}

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
        int fetched = 0;
        int inserted = 0;
        int skipped = 0;
        for (String symbol : symbols) {
            int[] r = fetchForSymbol(symbol, today.minusDays(7), today);
            if (r == null) {
                continue; // provider failed — don't count as fetched
            }
            fetched++;
            inserted += r[0];
            skipped += r[1];
        }
        log.info("News fetch complete: {} symbols fetched, {} inserted, {} skipped",
                fetched, inserted, skipped);
        return new FetchResult(true, fetched, inserted, skipped);
    }

    /** @return [inserted, skippedDuplicates] per article, or null if the provider failed */
    private int[] fetchForSymbol(String symbol, LocalDate from, LocalDate to) {
        List<NewsItem> items;
        try {
            items = newsProvider.fetchCompanyNews(symbol, from, to);
        } catch (Exception e) {
            log.warn("News fetch failed for {} — skipping", symbol, e.getClass().getSimpleName());
            return null;
        }
        int ins = 0;
        int skp = 0;
        for (NewsItem item : items) {
            if (persistence.persist(symbol, item) > 0) {
                ins++;
            } else {
                skp++;
            }
        }
        return new int[]{ins, skp};
    }
}
