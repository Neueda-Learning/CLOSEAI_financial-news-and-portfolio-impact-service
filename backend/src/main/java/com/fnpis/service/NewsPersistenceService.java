package com.fnpis.service;

import com.fnpis.domain.ArticleSecurityLink;
import com.fnpis.domain.MatchMethod;
import com.fnpis.domain.NewsArticle;
import com.fnpis.integration.NewsItem;
import com.fnpis.repository.ArticleSecurityLinkRepository;
import com.fnpis.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists one news fetch for one symbol.
 *
 * <p>Separated from {@link NewsFetchService} so {@code @Transactional}
 * actually takes effect (Spring AOP proxies only intercept cross-bean calls).
 * One call atomically writes the article and its link — both succeed or neither does.
 */
@Service
public class NewsPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(NewsPersistenceService.class);
    private final NewsArticleRepository articleRepo;
    private final ArticleSecurityLinkRepository linkRepo;

    public NewsPersistenceService(NewsArticleRepository articleRepo, ArticleSecurityLinkRepository linkRepo) {
        this.articleRepo = articleRepo;
        this.linkRepo = linkRepo;
    }

    /**
     * Persists one news item for one symbol.
     *
     * <p>If the article already exists (same externalId), it is reused
     * rather than re-inserted. The link is checked independently per
     * symbol, so the same news story can be linked to multiple tickers
     * (EC-24: one chip-sector story affects AAPL and NVDA).
     *
     * @return 1 if a new link was created, 0 if the link already existed
     */
    @Transactional
    public int persist(String symbol, NewsItem item) {
        NewsArticle article = articleRepo.findByExternalId(item.externalId())
                .orElseGet(() -> articleRepo.save(toEntity(item)));

        ArticleSecurityLink.Key key = new ArticleSecurityLink.Key(article.getId(), symbol);
        if (linkRepo.findById(key).isPresent()) {
            return 0;
        }
        linkRepo.save(new ArticleSecurityLink(
                article.getId(), symbol, MatchMethod.SYMBOL_EXACT));
        return 1;
    }

    private NewsArticle toEntity(NewsItem item) {
        NewsArticle a = new NewsArticle();
        a.setExternalId(item.externalId());
        a.setHeadline(item.headline());
        a.setSource(item.source());
        a.setUrl(item.url());
        a.setPublishedAt(item.publishedAt());
        a.setSummary(item.summary());
        return a;
    }
}
