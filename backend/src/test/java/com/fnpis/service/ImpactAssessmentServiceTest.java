package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.domain.ArticleSecurityLink;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.MatchMethod;
import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.Portfolio;
import com.fnpis.domain.SentimentLabel;
import com.fnpis.domain.SentimentScore;
import com.fnpis.repository.ArticleSecurityLinkRepository;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.NewsArticleRepository;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.PriceBarRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SentimentScoreRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * How many {@code impact_assessment} rows one recompute writes, and when it
 * writes none.
 *
 * <p>The arithmetic is {@link ImpactEngineTest}'s job; this class tests the
 * orchestration around it - which (article, symbol, portfolio) triples become
 * rows and which are skipped. Every collaborator is mocked, so there is no
 * database and no Spring context.
 *
 * <p><b>What these tests cannot prove.</b> EC-15 and EC-24 fall out of the
 * article-x-holding intersection, and here that intersection is stubbed rather
 * than run against SQL: {@code links.findByArticleId} returns whatever the test
 * sets, and {@code weights.of(symbol)} decides membership. So these assert the
 * service reacts correctly to an intersection of a given shape, not that the
 * real join produces that shape (dev plan section 0 gap table). Testcontainers
 * is where the join semantics get verified.
 */
@ExtendWith(MockitoExtension.class)
class ImpactAssessmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-29T13:00:00Z");
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 29);
    private static final Long PORTFOLIO_ID = 1L;
    private static final Long ARTICLE_ID = 100L;
    private static final BigDecimal EPSILON = new BigDecimal("0.005");

    @Mock private NewsArticleRepository articles;
    @Mock private ArticleSecurityLinkRepository links;
    @Mock private SentimentScoreRepository sentiments;
    @Mock private PriceBarRepository bars;
    @Mock private PriceQuoteRepository quotes;
    @Mock private ImpactAssessmentRepository assessments;
    @Mock private PortfolioRepository portfolios;
    @Mock private PortfolioService portfolioService;
    @Mock private AttributionDateResolver attributionDates;

    private ImpactAssessmentService service;

    @BeforeEach
    void setUp() {
        service = new ImpactAssessmentService(
                articles, links, sentiments, bars, quotes, assessments,
                portfolios, portfolioService, attributionDates, new ImpactEngine(), EPSILON);

        // One portfolio, one article charged to the session, one positive verdict.
        // Each test overrides the links and the weights to shape the intersection.
        when(portfolios.findAll()).thenReturn(List.of(portfolio(PORTFOLIO_ID)));
        when(articles.findByPublishedAtBetween(any(), any()))
                .thenReturn(List.of(article(ARTICLE_ID, NOW)));
        when(attributionDates.resolve(any(Instant.class))).thenReturn(SESSION);
        when(sentiments.findByArticleId(ARTICLE_ID))
                .thenReturn(Optional.of(sentiment(ARTICLE_ID)));
    }

    /**
     * The price and upsert lookups, stubbed only for the test that gets far
     * enough to make them. EC-15 and EC-13 both bail before any price read - that
     * early exit is the thing they assert - so stubbing these in {@code setUp}
     * would be dead stubbing there, and Mockito's strict mode is right to say so.
     *
     * <p>No price_bar rows and no quote: r is null, so the rows written here are
     * INCONCLUSIVE. That is fine - this test counts rows, it does not assert
     * alignment (ImpactEngineTest owns that).
     */
    private void stubNoPriceAndNoExistingRow() {
        when(bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(any(), any()))
                .thenReturn(List.of());
        when(assessments.findByArticleIdAndSymbolAndPortfolioIdAndAttributionDate(
                any(), any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("EC-15: article names a symbol the portfolio does not hold - no row")
    void ec15NotHeldWritesNothing() {
        // The story is about TSLA; the portfolio holds only NVDA.
        when(links.findByArticleId(ARTICLE_ID)).thenReturn(List.of(link(ARTICLE_ID, "TSLA")));
        when(portfolioService.weights(PORTFOLIO_ID, NOW))
                .thenReturn(weights(Map.of("NVDA", held("NVDA"))));

        int written = service.recomputeFor(SESSION, NOW);

        assertThat(written).isZero();
        verify(assessments, never()).save(any());
    }

    @Test
    @DisplayName("EC-24: one story on two held symbols - one row each, neither overwritten")
    void ec24MultipleHoldingsWriteOneEach() {
        // "Chip stocks rally" links NVDA and AMD; the portfolio holds both.
        when(links.findByArticleId(ARTICLE_ID))
                .thenReturn(List.of(link(ARTICLE_ID, "NVDA"), link(ARTICLE_ID, "AMD")));
        when(portfolioService.weights(PORTFOLIO_ID, NOW))
                .thenReturn(weights(Map.of("NVDA", held("NVDA"), "AMD", held("AMD"))));
        stubNoPriceAndNoExistingRow();

        int written = service.recomputeFor(SESSION, NOW);

        assertThat(written).isEqualTo(2);
        verify(assessments).save(argThatSymbolIs("NVDA"));
        verify(assessments).save(argThatSymbolIs("AMD"));
    }

    @Test
    @DisplayName("EC-13: held but no quote (assessable() false) - skipped, not a zero-weight row")
    void ec13NoQuoteWritesNothing() {
        // NVDA is held but has never been quoted: weight and market value are
        // null, assessable() is false. holding_weight is NOT NULL and a zero
        // would read as "no impact" rather than "cannot value" (module A 4.2).
        when(links.findByArticleId(ARTICLE_ID)).thenReturn(List.of(link(ARTICLE_ID, "NVDA")));
        when(portfolioService.weights(PORTFOLIO_ID, NOW))
                .thenReturn(weights(Map.of("NVDA", unquoted("NVDA"))));

        int written = service.recomputeFor(SESSION, NOW);

        assertThat(written).isZero();
        verify(assessments, never()).save(any());
    }

    // --- fixtures -----------------------------------------------------------

    private static Portfolio portfolio(Long id) {
        Portfolio p = new Portfolio("Test");
        p.setId(id);
        return p;
    }

    private static NewsArticle article(Long id, Instant publishedAt) {
        NewsArticle a = new NewsArticle();
        a.setId(id);
        a.setPublishedAt(publishedAt);
        return a;
    }

    private static SentimentScore sentiment(Long articleId) {
        SentimentScore s = new SentimentScore();
        s.setArticleId(articleId);
        s.setLabel(SentimentLabel.POSITIVE);
        s.setScore(new BigDecimal("0.7000"));
        s.setConfidence(new BigDecimal("0.8000"));
        s.setModelVersion("stub-v1");
        return s;
    }

    private static ArticleSecurityLink link(Long articleId, String symbol) {
        return new ArticleSecurityLink(articleId, symbol, MatchMethod.SYMBOL_EXACT);
    }

    /** A held, quoted position: assessable() is true. */
    private static PositionWeight held(String symbol) {
        return new PositionWeight(symbol, new BigDecimal("100"),
                new BigDecimal("38900.25"), new BigDecimal("0.302842"));
    }

    /** Held but never quoted (EC-13): weight and market value null, assessable() false. */
    private static PositionWeight unquoted(String symbol) {
        return new PositionWeight(symbol, new BigDecimal("100"), null, null);
    }

    private static PortfolioWeights weights(Map<String, PositionWeight> bySymbol) {
        return new PortfolioWeights(bySymbol, new BigDecimal("128450.75"), NOW, false);
    }

    private static ImpactAssessment argThatSymbolIs(String symbol) {
        return org.mockito.ArgumentMatchers.argThat(
                row -> row != null && symbol.equals(row.getSymbol()));
    }
}
