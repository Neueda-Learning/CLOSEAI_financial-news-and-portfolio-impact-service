package com.fnpis.service;

import com.fnpis.api.internal.dto.NewsDetailResponse;
import com.fnpis.api.internal.dto.NewsListRow;
import com.fnpis.common.PagedResponse;
import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.SentimentLabel;
import com.fnpis.domain.SentimentScore;
import com.fnpis.repository.ArticleSecurityLinkRepository;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.NewsArticleRepository;
import com.fnpis.repository.SentimentScoreRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Read-only news access for the API layer (C3, C4, C5).
 *
 * <p>Never calls a provider — serves whatever the fetch jobs have landed
 * (architecture decision 2). Sentiment may be null when analysis is pending.
 */
@Service
public class NewsReadService {

    private static final Logger log = LoggerFactory.getLogger(NewsReadService.class);

    private final NewsArticleRepository articleRepo;
    private final SentimentScoreRepository sentimentRepo;
    private final ArticleSecurityLinkRepository linkRepo;
    private final ImpactAssessmentRepository impactRepo;

    public NewsReadService(
            NewsArticleRepository articleRepo,
            SentimentScoreRepository sentimentRepo,
            ArticleSecurityLinkRepository linkRepo,
            ImpactAssessmentRepository impactRepo) {
        this.articleRepo = articleRepo;
        this.sentimentRepo = sentimentRepo;
        this.linkRepo = linkRepo;
        this.impactRepo = impactRepo;
    }

    /**
     * Paginated news list with optional symbol, sentiment, and date-range filters (C3, C4).
     */
    public PagedResponse<NewsListRow> list(String symbol, String sentimentLabel,
            Instant from, Instant to, int page, int size) {
        int p = Math.max(page, 1) - 1; // 0-based for Spring Data
        int s = Math.min(size, 100);
        SentimentLabel sl = parseLabel(sentimentLabel);

        Page<NewsArticle> result = articleRepo.findFiltered(symbol, sl, from, to,
                PageRequest.of(p, s));

        List<NewsListRow> content = result.getContent().stream()
                .map(this::toListRow)
                .toList();

        return new PagedResponse<>(content, page, s,
                result.getTotalElements(), result.getTotalPages(),
                null, false);
    }

    /**
     * One article with full detail (C5).
     *
     * @return the article or null when not found
     */
    public NewsDetailResponse detail(Long id) {
        return articleRepo.findById(id)
                .map(this::toDetail)
                .orElse(null);
    }

    private NewsListRow toListRow(NewsArticle a) {
        List<String> symbols = linkRepo.findByArticleId(a.getId())
                .stream()
                .map(l -> l.getSymbol())
                .toList();
        SentimentScore ss = sentimentRepo.findByArticleId(a.getId()).orElse(null);
        NewsListRow.SentimentSummary sentiment = ss == null ? null
                : new NewsListRow.SentimentSummary(
                        ss.getLabel().name(), ss.getScore().doubleValue(),
                        ss.getConfidence().doubleValue(), ss.getModelVersion());
        boolean hasImpact = impactRepo.existsByArticleId(a.getId());
        return new NewsListRow(a.getId(), a.getHeadline(), a.getSource(),
                a.getUrl(), a.getPublishedAt(), symbols, sentiment, hasImpact);
    }

    private NewsDetailResponse toDetail(NewsArticle a) {
        List<String> symbols = linkRepo.findByArticleId(a.getId())
                .stream()
                .map(l -> l.getSymbol())
                .toList();
        SentimentScore ss = sentimentRepo.findByArticleId(a.getId()).orElse(null);
        NewsListRow.SentimentSummary sentiment = ss == null ? null
                : new NewsListRow.SentimentSummary(
                        ss.getLabel().name(), ss.getScore().doubleValue(),
                        ss.getConfidence().doubleValue(), ss.getModelVersion());
        boolean hasImpact = impactRepo.existsByArticleId(a.getId());
        return new NewsDetailResponse(a.getId(), a.getHeadline(), a.getSource(),
                a.getUrl(), a.getSummary(), a.getImage(),
                a.getPublishedAt(), a.getFetchedAt(), symbols, sentiment, hasImpact);
    }

    private SentimentLabel parseLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        try {
            return SentimentLabel.valueOf(label.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
