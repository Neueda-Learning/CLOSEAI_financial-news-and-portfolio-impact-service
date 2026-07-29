package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The single-portfolio recompute the admin endpoint calls, and the lock guarding
 * both entry points.
 *
 * <p>Separate from {@link ImpactAssessmentServiceTest} because that class stubs
 * {@code portfolios.findAll()} for the all-portfolios walk, which this path never
 * makes - sharing the fixture would leave dead stubbing that Mockito's strict
 * mode is right to reject.
 */
@ExtendWith(MockitoExtension.class)
class ImpactRecomputeScopeTest {

    private static final Instant NOW = Instant.parse("2026-07-29T13:00:00Z");
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 29);
    private static final Long PORTFOLIO_ID = 7L;

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
                portfolios, portfolioService, attributionDates, new ImpactEngine(),
                new BigDecimal("0.005"));
    }

    /** An empty portfolio - the holdings are irrelevant to what these tests assert. */
    private static PortfolioWeights noHoldings() {
        return new PortfolioWeights(Map.of(), new BigDecimal("128450.75"), NOW, false);
    }

    @Nested
    @DisplayName("recomputing one portfolio")
    class One {

        @Test
        @DisplayName("an unknown portfolio is a 404, not a silent zero")
        void unknownPortfolioRejected() {
            when(portfolios.existsById(PORTFOLIO_ID)).thenReturn(false);

            // Returning 0 would read as "recomputed, nothing to do" for what is
            // actually a typo'd id.
            assertThatThrownBy(() -> service.recomputeOne(PORTFOLIO_ID, SESSION, NOW))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.PORTFOLIO_NOT_FOUND);

            // Rejected before any work: no article scan for a portfolio that is
            // not there.
            verify(articles, never()).findByPublishedAtBetween(any(), any());
        }

        @Test
        @DisplayName("a known portfolio with no news writes nothing and does not fail")
        void quietSessionWritesNothing() {
            when(portfolios.existsById(PORTFOLIO_ID)).thenReturn(true);
            when(articles.findByPublishedAtBetween(any(), any())).thenReturn(List.of());
            when(portfolioService.weights(PORTFOLIO_ID, NOW)).thenReturn(noHoldings());

            assertThat(service.recomputeOne(PORTFOLIO_ID, SESSION, NOW)).isZero();

            verify(assessments, never()).save(any());
        }

        @Test
        @DisplayName("it never walks the other portfolios")
        void staysInScope() {
            when(portfolios.existsById(PORTFOLIO_ID)).thenReturn(true);
            when(articles.findByPublishedAtBetween(any(), any())).thenReturn(List.of());
            when(portfolioService.weights(PORTFOLIO_ID, NOW)).thenReturn(noHoldings());

            service.recomputeOne(PORTFOLIO_ID, SESSION, NOW);

            // findAll is the all-portfolios path. Reaching it here would rewrite
            // sessions the admin caller did not ask about.
            verify(portfolios, never()).findAll();
        }
    }

    @Nested
    @DisplayName("the execution lock")
    class Lock {

        @Test
        @DisplayName("only one runner holds it at a time")
        void mutualExclusion() {
            assertThat(service.tryAcquire()).isTrue();
            // Two concurrent recomputes would each upsert the same rows, and the
            // loser's numbers would replace the winner's rather than colliding on
            // the unique constraint (EC-20, EC-23).
            assertThat(service.tryAcquire()).isFalse();

            service.release();
            assertThat(service.tryAcquire()).isTrue();
        }
    }
}
