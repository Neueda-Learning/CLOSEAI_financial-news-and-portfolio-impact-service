package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.api.internal.dto.ImpactViewResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.PriceBar;
import com.fnpis.domain.PricePoint;
import com.fnpis.domain.PriceQuote;
import com.fnpis.domain.Security;
import com.fnpis.domain.SentimentLabel;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The linked view's binding rules.
 *
 * <p>Most of these tests exist to prevent one specific class of bug: returning a
 * curve that belongs to a different symbol than the caller is looking at. The
 * contract calls that out as the endpoint's easiest mistake, and it is the kind
 * that looks fine on screen - a chart appears, the numbers move, and the story is
 * being told about the wrong stock.
 */
@ExtendWith(MockitoExtension.class)
class ImpactViewServiceTest {

    private static final Long ARTICLE_ID = 8842L;
    private static final Long PORTFOLIO_ID = 1L;
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);
    private static final Instant PUBLISHED = Instant.parse("2026-07-27T12:31:00Z");
    private static final Instant COMPUTED = Instant.parse("2026-07-27T16:05:00Z");

    @Mock private NewsArticleRepository articles;
    @Mock private SentimentScoreRepository sentiments;
    @Mock private ImpactAssessmentRepository assessments;
    @Mock private SecurityRepository securities;
    @Mock private PortfolioRepository portfolios;
    @Mock private PricePointRepository points;
    @Mock private PriceQuoteRepository quotes;
    @Mock private PriceBarRepository bars;
    @Mock private AttributionDateResolver attributionDates;

    private ImpactViewService service;

    @BeforeEach
    void setUp() {
        service = new ImpactViewService(
                articles, sentiments, assessments, securities, portfolios,
                points, quotes, bars, attributionDates);
    }

    private static NewsArticle article() {
        NewsArticle a = new NewsArticle();
        a.setId(ARTICLE_ID);
        a.setHeadline("Nvidia beats Q2 estimates, raises guidance");
        a.setSource("Reuters");
        a.setUrl("https://example.com/article/8842");
        a.setPublishedAt(PUBLISHED);
        return a;
    }

    private static ImpactAssessment row(String symbol, String valueImpact) {
        ImpactAssessment r = new ImpactAssessment();
        r.setArticleId(ARTICLE_ID);
        r.setPortfolioId(PORTFOLIO_ID);
        r.setSymbol(symbol);
        r.setAttributionDate(SESSION);
        r.setHoldingWeight(new BigDecimal("0.303"));
        r.setPriceChangeRatio(new BigDecimal("0.041500"));
        r.setExpectedImpact(new BigDecimal("0.192"));
        r.setObservedContribution(new BigDecimal("1.258"));
        r.setValueImpact(valueImpact == null ? null : new BigDecimal(valueImpact));
        r.setDirection(Direction.POSITIVE);
        r.setAlignment(Alignment.CONFIRMED);
        r.setComputedAt(COMPUTED);
        return r;
    }

    private static PricePoint point(String at, String price) {
        PricePoint p = new PricePoint();
        p.setSymbol("NVDA");
        p.setCapturedAt(Instant.parse(at));
        p.setPrice(new BigDecimal(price));
        return p;
    }

    /** The article and portfolio both exist - the precondition for every test here. */
    private void subjectsExist() {
        when(articles.findById(ARTICLE_ID)).thenReturn(Optional.of(article()));
        when(portfolios.existsById(PORTFOLIO_ID)).thenReturn(true);
    }

    private void assessed(ImpactAssessment... rows) {
        when(assessments.findByArticleIdAndPortfolioId(ARTICLE_ID, PORTFOLIO_ID))
                .thenReturn(List.of(rows));
    }

    /** No bars and no quote: the baseline is absent, which most tests do not assert. */
    private void noPriceHistory() {
        when(bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(any(), any()))
                .thenReturn(List.of());
        when(quotes.findById(any())).thenReturn(Optional.empty());
    }

    private void noPoints() {
        when(points.findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Nested
    @DisplayName("the curve is bound to selectedSymbol, never guessed")
    class SymbolBinding {

        @Test
        @DisplayName("omitting symbol selects the biggest mover and the curve follows it")
        void defaultsToBiggestMover() {
            subjectsExist();
            // AMD is listed first but moved less. A view that trusted row order
            // would chart AMD while the page talks about NVDA.
            assessed(row("AMD", "508.80"), row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, null);

            assertThat(view.selectedSymbol()).isEqualTo("NVDA");
            assertThat(view.priceSeries().symbol()).isEqualTo("NVDA");
            // The switcher shows both, ordered by magnitude.
            assertThat(view.impactedSymbols()).containsExactly("NVDA", "AMD");
            verify(points).findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(
                    eq("NVDA"), any(), any());
        }

        @Test
        @DisplayName("an explicit symbol overrides the biggest mover")
        void explicitSymbolWins() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"), row("AMD", "508.80"));
            noPriceHistory();
            noPoints();

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, "AMD");

            assertThat(view.selectedSymbol()).isEqualTo("AMD");
            assertThat(view.priceSeries().symbol()).isEqualTo("AMD");
        }

        @Test
        @DisplayName("a lowercase symbol still matches")
        void symbolIsCaseInsensitive() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();

            assertThat(service.view(ARTICLE_ID, PORTFOLIO_ID, "nvda").selectedSymbol())
                    .isEqualTo("NVDA");
        }

        @Test
        @DisplayName("a symbol this story did not impact is a 404, not a silent fallback")
        void unimpactedSymbolRejected() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));

            // Falling back to NVDA's curve here would answer a question the caller
            // did not ask, and the page would caption it with the wrong symbol.
            assertThatThrownBy(() -> service.view(ARTICLE_ID, PORTFOLIO_ID, "TSLA"))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.SECURITY_NOT_FOUND);

            verify(points, never()).findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(
                    any(), any(), any());
        }

        @Test
        @DisplayName("a row with no value impact is listed but never chosen over one with")
        void unrankableSortsLast() {
            subjectsExist();
            assessed(row("AMD", null), row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, null);

            assertThat(view.selectedSymbol()).isEqualTo("NVDA");
            // Still rendered - it is a real assessment, just not a rankable one.
            assertThat(view.impacts()).hasSize(2);
            assertThat(view.impactedSymbols()).containsExactly("NVDA", "AMD");
        }
    }

    @Nested
    @DisplayName("the news marker sits on the chart's own axis")
    class NewsMarker {

        @Test
        @DisplayName("it snaps to the first point at or after the story broke")
        void snapsForward() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            when(points.findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(
                    any(), any(), any()))
                    .thenReturn(List.of(
                            point("2026-07-27T12:00:00Z", "121.85"),
                            point("2026-07-27T12:35:00Z", "125.60")));

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, null);

            // Published 12:31, and no capture exists at that instant. The marker
            // takes 12:35 rather than 12:31, so the line lands on a plotted point
            // instead of between two of them.
            assertThat(view.priceSeries().newsMarker())
                    .isEqualTo(Instant.parse("2026-07-27T12:35:00Z"));
        }

        @Test
        @DisplayName("a story after the last capture gets no marker")
        void nullWhenOffTheEnd() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            when(points.findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(
                    any(), any(), any()))
                    .thenReturn(List.of(point("2026-07-27T11:00:00Z", "121.85")));

            // Drawing it past the end of the line would imply a reaction that was
            // never recorded.
            assertThat(service.view(ARTICLE_ID, PORTFOLIO_ID, null)
                    .priceSeries().newsMarker()).isNull();
        }
    }

    @Nested
    @DisplayName("the chart baseline")
    class PreviousClose {

        @Test
        @DisplayName("a settled bar pair supplies the prior close")
        void fromBars() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPoints();
            PriceBar today = new PriceBar();
            today.setSymbol("NVDA");
            today.setTradeDate(SESSION);
            today.setClosePrice(new BigDecimal("126.44"));
            PriceBar prior = new PriceBar();
            prior.setSymbol("NVDA");
            prior.setTradeDate(SESSION.minusDays(1));
            prior.setClosePrice(new BigDecimal("121.40"));
            when(bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(
                    eq("NVDA"), eq(SESSION)))
                    .thenReturn(List.of(today, prior));

            assertThat(service.view(ARTICLE_ID, PORTFOLIO_ID, null)
                    .priceSeries().previousClose()).isEqualByComparingTo("121.40");
        }

        @Test
        @DisplayName("with no snapshot yet it falls back to the live quote")
        void fallsBackToQuote() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPoints();
            when(bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(
                    any(), any())).thenReturn(List.of());
            PriceQuote quote = new PriceQuote();
            quote.setSymbol("NVDA");
            quote.setPreviousClose(new BigDecimal("121.40"));
            when(quotes.findById("NVDA")).thenReturn(Optional.of(quote));

            assertThat(service.view(ARTICLE_ID, PORTFOLIO_ID, null)
                    .priceSeries().previousClose()).isEqualByComparingTo("121.40");
        }
    }

    @Nested
    @DisplayName("what the view still returns when data is missing")
    class Degradation {

        @Test
        @DisplayName("a story impacting nothing held returns no curve, not an error")
        void noImpactsNoCurve() {
            subjectsExist();
            assessed();
            when(attributionDates.resolve(PUBLISHED)).thenReturn(SESSION);

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, null);

            assertThat(view.impacts()).isEmpty();
            assertThat(view.selectedSymbol()).isNull();
            assertThat(view.priceSeries()).isNull();
            // The session still resolves, from the story's own timestamp.
            assertThat(view.attributionDate()).isEqualTo(SESSION);
            assertThat(view.stale()).isTrue();
            assertThat(view.asOf()).isNull();
        }

        @Test
        @DisplayName("an unscored headline renders with a null sentiment")
        void missingSentiment() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();
            when(sentiments.findByArticleId(ARTICLE_ID)).thenReturn(Optional.empty());

            ImpactViewResponse view = service.view(ARTICLE_ID, PORTFOLIO_ID, null);

            assertThat(view.article().sentiment()).isNull();
            // The impacts are still there - they were computed when a verdict did
            // exist, and dropping them would empty the page.
            assertThat(view.impacts()).hasSize(1);
        }

        @Test
        @DisplayName("the stored verdict travels with its model version")
        void sentimentCarriesModelVersion() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();
            SentimentScore verdict = new SentimentScore();
            verdict.setLabel(SentimentLabel.POSITIVE);
            verdict.setScore(new BigDecimal("0.72"));
            verdict.setConfidence(new BigDecimal("0.88"));
            verdict.setModelVersion("agent-v1");
            when(sentiments.findByArticleId(ARTICLE_ID)).thenReturn(Optional.of(verdict));

            var sentiment = service.view(ARTICLE_ID, PORTFOLIO_ID, null).article().sentiment();

            assertThat(sentiment.label()).isEqualTo(SentimentLabel.POSITIVE);
            // Without this, a verdict cannot be traced to the model that made it.
            assertThat(sentiment.modelVersion()).isEqualTo("agent-v1");
        }

        @Test
        @DisplayName("an unknown company name falls back to the symbol")
        void nameFallsBack() {
            subjectsExist();
            assessed(row("XYZQ", "10.00"));
            noPriceHistory();
            noPoints();
            when(securities.findBySymbolIn(any())).thenReturn(List.of());

            assertThat(service.view(ARTICLE_ID, PORTFOLIO_ID, null)
                    .impacts().get(0).companyName()).isEqualTo("XYZQ");
        }

        @Test
        @DisplayName("the ratio is published as a percentage")
        void ratioBecomesPercentage() {
            subjectsExist();
            assessed(row("NVDA", "1614.36"));
            noPriceHistory();
            noPoints();
            Security nvda = new Security();
            nvda.setSymbol("NVDA");
            nvda.setCompanyName("NVIDIA Corporation");
            when(securities.findBySymbolIn(any())).thenReturn(List.of(nvda));

            var first = service.view(ARTICLE_ID, PORTFOLIO_ID, null).impacts().get(0);

            assertThat(first.companyName()).isEqualTo("NVIDIA Corporation");
            assertThat(first.priceChangePct()).isEqualTo(4.15);
        }
    }

    @Nested
    @DisplayName("missing subjects are 404s")
    class NotFound {

        @Test
        @DisplayName("an unknown article")
        void unknownArticle() {
            when(articles.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.view(404L, PORTFOLIO_ID, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.ARTICLE_NOT_FOUND);
        }

        @Test
        @DisplayName("an unknown portfolio, checked before any assessment is read")
        void unknownPortfolio() {
            when(articles.findById(ARTICLE_ID)).thenReturn(Optional.of(article()));
            when(portfolios.existsById(anyLong())).thenReturn(false);

            assertThatThrownBy(() -> service.view(ARTICLE_ID, 404L, null))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.PORTFOLIO_NOT_FOUND);

            // An empty impacts list would imply the portfolio exists and simply
            // was not affected.
            verify(assessments, never()).findByArticleIdAndPortfolioId(any(), any());
        }
    }
}
