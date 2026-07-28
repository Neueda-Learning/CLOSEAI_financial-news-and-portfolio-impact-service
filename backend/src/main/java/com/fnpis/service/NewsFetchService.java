package com.fnpis.service;

import com.fnpis.domain.ArticleSecurityLink;
import com.fnpis.domain.MatchMethod;
import com.fnpis.domain.NewsArticle;
import com.fnpis.integration.NewsItem;
import com.fnpis.integration.NewsProvider;
import com.fnpis.repository.ArticleSecurityLinkRepository;
import com.fnpis.repository.NewsArticleRepository;
import com.fnpis.repository.SecurityRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NewsFetchService {

    private static final Logger log = LoggerFactory.getLogger(NewsFetchService.class);
    private final NewsProvider newsProvider;
    private final NewsArticleRepository articleRepo;
    private final ArticleSecurityLinkRepository linkRepo;
    private final SecurityRepository securityRepo;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NewsFetchService(
            NewsProvider newsProvider,
            NewsArticleRepository articleRepo,
            ArticleSecurityLinkRepository linkRepo,
            SecurityRepository securityRepo) {
        this.newsProvider = newsProvider;
        this.articleRepo = articleRepo;
        this.linkRepo = linkRepo;
        this.securityRepo = securityRepo;
    }

    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    public void release() {
        running.set(false);
    }

    /**
     * Fetches news for every symbol in the watchlist.
     * One symbol = one transaction.
     *
     * @return total number of new articles persisted
     */
    public int fetchAll() {
        List<String> symbols = securityRepo.findAll()
                .stream()
                .map(s -> s.getSymbol())
                .toList();
        LocalDate today = LocalDate.now();
        int totalNew = 0;
        for (String symbol : symbols) {
            totalNew += fetchForSymbol(symbol, today.minusDays(7), today);
        }
        log.info("News fetch complete: {} new articles across {} symbols",
                totalNew, symbols.size());
        return totalNew;
    }

    @Transactional
    int fetchForSymbol(String symbol, LocalDate from, LocalDate to) {
        List<NewsItem> items;
        try {
            items = newsProvider.fetchCompanyNews(symbol, from, to);
        } catch (Exception e) {
            log.warn("News fetch failed for {} — skipping", symbol, e);
            return 0;
        }
        int count = 0;
        for (NewsItem item : items) {
            if (articleRepo.findByExternalId(item.externalId()).isPresent()) {
                continue;
            }
            NewsArticle article = toEntity(item);
            NewsArticle saved = articleRepo.save(article);
            linkRepo.save(new ArticleSecurityLink(
                    saved.getId(), symbol, MatchMethod.SYMBOL_EXACT));
            count++;
        }
        return count;
    }

    private NewsArticle toEntity(NewsItem item) {
        NewsArticle a = new NewsArticle();
        a.setExternalId(item.externalId());
        a.setHeadline(item.headline());
        a.setSource(item.source());
        a.setUrl(item.url());
        a.setPublishedAt(item.publishedAt());
        return a;
    }
}
