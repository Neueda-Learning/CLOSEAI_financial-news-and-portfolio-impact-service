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
     * Persists one symbol's news batch. Already-fetched articles (by externalId)
     * are skipped. New articles are saved atomically with their security link.
     *
     * @return number of new articles persisted
     */
    @Transactional
    public int persist(String symbol, NewsItem item) {
        if (articleRepo.findByExternalId(item.externalId()).isPresent()) {
            return 0;
        }
        NewsArticle article = toEntity(item);
        NewsArticle saved = articleRepo.save(article);
        linkRepo.save(new ArticleSecurityLink(
                saved.getId(), symbol, MatchMethod.SYMBOL_EXACT));
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
