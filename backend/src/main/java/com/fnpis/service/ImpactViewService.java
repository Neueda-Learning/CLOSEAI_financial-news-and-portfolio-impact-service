package com.fnpis.service;

import com.fnpis.api.internal.dto.ImpactViewResponse;
import com.fnpis.common.Freshness;
import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.PriceBar;
import com.fnpis.domain.PriceQuote;
import com.fnpis.domain.Security;
import com.fnpis.domain.SentimentScore;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.NewsArticleRepository;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.PriceBarRepository;
import com.fnpis.repository.PricePointRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SecurityRepository;
import com.fnpis.repository.SentimentScoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The linked view behind the demo's headline moment (F4, E1-E4).
 *
 * <p>One request returns the story, its verdict, every impacted holding and one
 * intraday curve, because the contract makes this endpoint solely responsible for
 * that page. Served entirely from persisted rows (decisions 2 and 3): no provider
 * call, so a dead upstream shows old data with {@code stale: true} rather than
 * breaking the demo.
 *
 * <p><b>One curve, not N.</b> Intraday points are fetched per symbol, so returning
 * every impacted symbol's curve would multiply the quote cost for data the user
 * is not looking at. The caller switches symbols with {@code ?symbol=} instead.
 */
@Service
public class ImpactViewService {

    private final NewsArticleRepository articles;
    private final SentimentScoreRepository sentiments;
    private final ImpactAssessmentRepository assessments;
    private final SecurityRepository securities;
    private final PortfolioRepository portfolios;
    private final PricePointRepository points;
    private final PriceQuoteRepository quotes;
    private final PriceBarRepository bars;
    private final AttributionDateResolver attributionDates;

    ImpactViewService(
            NewsArticleRepository articles,
            SentimentScoreRepository sentiments,
            ImpactAssessmentRepository assessments,
            SecurityRepository securities,
            PortfolioRepository portfolios,
            PricePointRepository points,
            PriceQuoteRepository quotes,
            PriceBarRepository bars,
            AttributionDateResolver attributionDates) {
        this.articles = articles;
        this.sentiments = sentiments;
        this.assessments = assessments;
        this.securities = securities;
        this.portfolios = portfolios;
        this.points = points;
        this.quotes = quotes;
        this.bars = bars;
        this.attributionDates = attributionDates;
    }

    /**
     * Assembles the view for one story against one portfolio.
     *
     * @param requestedSymbol which curve to return; null picks the biggest mover
     * @throws ApiException 404 when the article or the portfolio does not exist
     */
    @Transactional(readOnly = true)
    public ImpactViewResponse view(Long articleId, Long portfolioId, String requestedSymbol) {
        NewsArticle article = articles.findById(articleId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.ARTICLE_NOT_FOUND, "No article with id " + articleId));
        if (!portfolios.existsById(portfolioId)) {
            throw ApiException.portfolioNotFound(portfolioId);
        }

        // Impact rows carry the weights, so they come from the portfolio-scoped
        // query rather than from the article's links: a symbol the story touched
        // but the portfolio does not hold has no assessment and no place here.
        List<ImpactAssessment> rows = assessments
                .findByArticleIdAndPortfolioId(articleId, portfolioId)
                .stream()
                .sorted(byImpactMagnitude())
                .toList();

        List<String> impactedSymbols = rows.stream()
                .map(ImpactAssessment::getSymbol)
                .distinct()
                .toList();
        String selected = selectSymbol(requestedSymbol, impactedSymbols);
        LocalDate session = rows.isEmpty()
                ? attributionDates.resolve(article.getPublishedAt())
                : rows.get(0).getAttributionDate();

        Map<String, String> names = companyNames(impactedSymbols);
        Freshness freshness = freshnessOf(rows);

        return new ImpactViewResponse(
                articleOf(article),
                session,
                impactedSymbols,
                selected,
                rows.stream().map(row -> ImpactRowMapper.toRow(row, names)).toList(),
                selected == null ? null : priceSeries(selected, session, article.getPublishedAt()),
                freshness.asOf(),
                freshness.stale());
    }

    /**
     * Biggest absolute value impact first, so the switcher opens on the row worth
     * looking at. Unrankable rows (no return, so no value impact) sort last rather
     * than being dropped - they are real assessments.
     */
    private Comparator<ImpactAssessment> byImpactMagnitude() {
        return Comparator.comparing(
                (ImpactAssessment row) -> row.getValueImpact() == null
                        ? BigDecimal.valueOf(-1)
                        : row.getValueImpact().abs(),
                Comparator.reverseOrder());
    }

    /**
     * Which curve to return.
     *
     * <p>An explicit {@code symbol} the story did not impact is a 404 rather than
     * a silent fallback to the biggest mover: answering with a different symbol's
     * curve than the one asked for is exactly the mix-up the contract warns about.
     */
    private String selectSymbol(String requested, List<String> impacted) {
        if (requested == null || requested.isBlank()) {
            // Already sorted, so element 0 is the biggest mover. Null when the
            // story impacted nothing in this portfolio.
            return impacted.isEmpty() ? null : impacted.get(0);
        }
        String normalised = requested.toUpperCase(java.util.Locale.ROOT);
        if (!impacted.contains(normalised)) {
            throw new ApiException(
                    ErrorCode.SECURITY_NOT_FOUND,
                    "This article has no assessed impact on " + normalised
                            + " for this portfolio");
        }
        return normalised;
    }

    /**
     * The selected symbol's intraday curve for the session.
     *
     * <p>The window is the session's own UTC day. Anything wider would mix two
     * days of points into one line, and the marker would land ambiguously.
     */
    private ImpactViewResponse.PriceSeries priceSeries(
            String symbol, LocalDate session, Instant publishedAt) {
        Instant from = session.minusDays(5).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = session.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<ImpactViewResponse.PriceSeries.Point> curve = points
                .findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(symbol, from, to)
                .stream()
                .map(p -> new ImpactViewResponse.PriceSeries.Point(
                        p.getCapturedAt(), p.getPrice()))
                .toList();

        return new ImpactViewResponse.PriceSeries(
                symbol,
                previousCloseOf(symbol, session),
                markerFor(curve, publishedAt),
                curve);
    }

    /**
     * Where the annotation line goes: the first captured point at or after the
     * story broke.
     *
     * <p>Snapped to a real point rather than passing {@code publishedAt} through,
     * so the line sits on the axis the chart actually plots. A story published
     * after the last capture of the day returns null - drawing the marker off the
     * end of the line would imply a price reaction that was never recorded.
     */
    private Instant markerFor(
            List<ImpactViewResponse.PriceSeries.Point> curve, Instant publishedAt) {
        if (publishedAt == null) {
            return null;
        }
        return curve.stream()
                .map(ImpactViewResponse.PriceSeries.Point::t)
                .filter(t -> !t.isBefore(publishedAt))
                .findFirst()
                .orElse(null);
    }

    /**
     * The session's prior close, for the chart baseline.
     *
     * <p>{@code price_bar} first, since it is the settled number; the live quote's
     * {@code previousClose} is the fallback for a session whose snapshot has not
     * run yet. Same two-tier order as the assessment's return derivation, so the
     * baseline and the assessed move agree on what "previous" meant.
     */
    private BigDecimal previousCloseOf(String symbol, LocalDate session) {
        List<PriceBar> recent =
                bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(symbol, session);
        if (recent.size() == 2 && recent.get(0).getTradeDate().equals(session)) {
            return recent.get(1).getClosePrice();
        }
        return quotes.findById(symbol).map(PriceQuote::getPreviousClose).orElse(null);
    }

    private ImpactViewResponse.Article articleOf(NewsArticle article) {
        Optional<SentimentScore> verdict = sentiments.findByArticleId(article.getId());
        return new ImpactViewResponse.Article(
                article.getId(),
                article.getHeadline(),
                article.getSource(),
                article.getUrl(),
                article.getPublishedAt(),
                verdict.map(s -> new ImpactViewResponse.Sentiment(
                                s.getLabel(),
                                score(s.getScore()),
                                score(s.getConfidence()),
                                s.getModelVersion()))
                        .orElse(null));
    }

    /**
     * A stored sentiment figure as a wire number, preserving null.
     *
     * <p>No rescaling: the validator already constrained the range, and rounding
     * a verdict on the way out would publish a number the model never produced.
     */
    private Double score(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private Map<String, String> companyNames(List<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        Set<String> wanted = Set.copyOf(symbols);
        Map<String, String> names = new HashMap<>();
        for (Security security : securities.findBySymbolIn(wanted)) {
            names.put(security.getSymbol(), security.getCompanyName());
        }
        return names;
    }

    /** Oldest computation in the set - a mixed set is only as fresh as its stalest row. */
    private Freshness freshnessOf(List<ImpactAssessment> rows) {
        return rows.stream()
                .map(ImpactAssessment::getComputedAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .map(Freshness::fresh)
                .orElseGet(() -> Freshness.stale(null));
    }
}
