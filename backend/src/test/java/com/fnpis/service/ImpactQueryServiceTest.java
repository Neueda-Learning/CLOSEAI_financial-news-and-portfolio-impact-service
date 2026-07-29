package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fnpis.api.internal.dto.ImpactRow;
import com.fnpis.api.internal.dto.ImpactSummaryResponse;
import com.fnpis.common.PagedResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import com.fnpis.domain.Holding;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.Security;
import com.fnpis.repository.HoldingRepository;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.SecurityRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * The read path's rules: what the aggregates mean and when they refuse to answer.
 *
 * <p>The division-by-zero traps get most of the attention here, because each one
 * has a correct null answer that is easy to accidentally turn into a misleading
 * zero - "0% agreement" and "no sample" look nothing alike to a lecturer reading
 * the demo screen.
 */
@ExtendWith(MockitoExtension.class)
class ImpactQueryServiceTest {

    private static final Long PORTFOLIO_ID = 7L;
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);
    private static final Instant COMPUTED = Instant.parse("2026-07-27T16:05:00Z");

    @Mock private ImpactAssessmentRepository assessments;
    @Mock private HoldingRepository holdings;
    @Mock private SecurityRepository securities;
    @Mock private PortfolioRepository portfolios;

    private ImpactQueryService service;

    @BeforeEach
    void setUp() {
        service = new ImpactQueryService(assessments, holdings, securities, portfolios);
    }

    /** An assessment row with the fields the read path actually reads. */
    private static ImpactAssessment row(
            String symbol, Alignment alignment, String valueImpact, String weight) {
        ImpactAssessment a = new ImpactAssessment();
        a.setSymbol(symbol);
        a.setPortfolioId(PORTFOLIO_ID);
        a.setAttributionDate(SESSION);
        a.setAlignment(alignment);
        a.setDirection(Direction.POSITIVE);
        a.setHoldingWeight(new BigDecimal(weight));
        a.setExpectedImpact(new BigDecimal("0.10"));
        a.setValueImpact(valueImpact == null ? null : new BigDecimal(valueImpact));
        a.setPriceChangeRatio(new BigDecimal("0.0415"));
        a.setObservedContribution(new BigDecimal("0.0126"));
        a.setComputedAt(COMPUTED);
        return a;
    }

    /** {@code n} rows of one alignment, so a sample size can be dialled exactly. */
    private static List<ImpactAssessment> rows(Alignment alignment, int n) {
        List<ImpactAssessment> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(row("S" + i, alignment, "100.00", "0.10"));
        }
        return out;
    }

    private static Holding holding(String symbol) {
        Holding h = new Holding();
        h.setPortfolioId(PORTFOLIO_ID);
        h.setSymbol(symbol);
        return h;
    }

    private void portfolioExists() {
        when(portfolios.existsById(PORTFOLIO_ID)).thenReturn(true);
    }

    private void sessionHas(List<ImpactAssessment> found) {
        when(assessments.findByPortfolioIdAndAttributionDate(PORTFOLIO_ID, SESSION))
                .thenReturn(found);
    }

    @Nested
    @DisplayName("the agreement rate refuses to answer on a thin sample")
    class AgreementRate {

        @Test
        @DisplayName("SC-008: fewer than 20 opinionated rows reports null, not a number")
        void thinSampleIsNull() {
            portfolioExists();
            // 19 confirmed: a perfect record, and still not enough to publish.
            sessionHas(rows(Alignment.CONFIRMED, 19));
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            ImpactSummaryResponse summary = service.summary(PORTFOLIO_ID, SESSION);

            assertThat(summary.directionAgreementRate()).isNull();
            // The sample still travels, so the frontend can say why the rate is
            // missing rather than just showing a blank.
            assertThat(summary.sampleSize()).isEqualTo(19);
            assertThat(summary.counts().confirmed()).isEqualTo(19);
        }

        @Test
        @DisplayName("at 20 rows the rate appears")
        void atThresholdReports() {
            portfolioExists();
            List<ImpactAssessment> found = new ArrayList<>(rows(Alignment.CONFIRMED, 15));
            found.addAll(rows(Alignment.DIVERGENT, 5));
            sessionHas(found);
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            ImpactSummaryResponse summary = service.summary(PORTFOLIO_ID, SESSION);

            assertThat(summary.sampleSize()).isEqualTo(20);
            assertThat(summary.directionAgreementRate()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("INCONCLUSIVE rows stay out of the denominator")
        void inconclusiveExcluded() {
            portfolioExists();
            List<ImpactAssessment> found = new ArrayList<>(rows(Alignment.CONFIRMED, 20));
            // A quiet day: 30 stories moved the price less than epsilon. Counting
            // these as disagreements would drag a perfect record down to 40%.
            found.addAll(rows(Alignment.INCONCLUSIVE, 30));
            sessionHas(found);
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            ImpactSummaryResponse summary = service.summary(PORTFOLIO_ID, SESSION);

            assertThat(summary.directionAgreementRate()).isEqualTo(1.0);
            assertThat(summary.sampleSize()).isEqualTo(20);
            assertThat(summary.counts().inconclusive()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("the aggregates survive an empty session")
    class EmptySession {

        @Test
        @DisplayName("no rows is an empty answer, not an error")
        void noRows() {
            portfolioExists();
            sessionHas(List.of());
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID))
                    .thenReturn(List.of(holding("NVDA")));

            ImpactSummaryResponse summary = service.summary(PORTFOLIO_ID, SESSION);

            assertThat(summary.sampleSize()).isZero();
            assertThat(summary.directionAgreementRate()).isNull();
            assertThat(summary.weightedSentiment()).isNull();
            assertThat(summary.topImpacted()).isEmpty();
            // Nothing computed means no capture time to report, and claiming
            // freshness for absent data would be a lie.
            assertThat(summary.asOf()).isNull();
            assertThat(summary.stale()).isTrue();
            // One holding, no stories: real 0% coverage, not a missing answer.
            assertThat(summary.newsCoverage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("EC-22: a portfolio holding nothing reports null coverage, not 0%")
        void noHoldingsIsNullCoverage() {
            portfolioExists();
            sessionHas(List.of());
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            // Zero of zero holdings covered is a division by zero. Reporting 0%
            // would claim we checked and found no coverage.
            assertThat(service.summary(PORTFOLIO_ID, SESSION).newsCoverage()).isNull();
        }
    }

    @Nested
    @DisplayName("coverage and top movers")
    class CoverageAndMovers {

        @Test
        @DisplayName("coverage counts held symbols a story touched")
        void coverage() {
            portfolioExists();
            sessionHas(List.of(row("NVDA", Alignment.CONFIRMED, "500.00", "0.30")));
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID))
                    .thenReturn(List.of(holding("NVDA"), holding("AMD"),
                            holding("TSLA"), holding("MSFT")));

            assertThat(service.summary(PORTFOLIO_ID, SESSION).newsCoverage()).isEqualTo(0.25);
        }

        @Test
        @DisplayName("movers rank by absolute impact but keep their sign")
        void ranksByMagnitudeKeepsSign() {
            portfolioExists();
            sessionHas(List.of(
                    row("NVDA", Alignment.CONFIRMED, "300.00", "0.10"),
                    row("TSLA", Alignment.DIVERGENT, "-892.10", "0.20"),
                    row("AMD", Alignment.CONFIRMED, "508.80", "0.10")));
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            List<com.fnpis.api.internal.dto.TopImpactedItem> top =
                    service.summary(PORTFOLIO_ID, SESSION).topImpacted();

            // The biggest mover is a loss - it must sort first and stay negative.
            assertThat(top).extracting("symbol").containsExactly("TSLA", "AMD", "NVDA");
            assertThat(top.get(0).valueImpact()).isEqualByComparingTo("-892.10");
        }

        @Test
        @DisplayName("a row with no value impact is not rankable but is not dropped")
        void unrankableRowKeptOutOfTop() {
            portfolioExists();
            sessionHas(List.of(
                    row("NVDA", Alignment.CONFIRMED, "300.00", "0.10"),
                    row("AMD", Alignment.INCONCLUSIVE, null, "0.10")));
            when(holdings.findByPortfolioIdOrderBySymbol(PORTFOLIO_ID)).thenReturn(List.of());

            ImpactSummaryResponse summary = service.summary(PORTFOLIO_ID, SESSION);

            assertThat(summary.topImpacted()).extracting("symbol").containsExactly("NVDA");
            // It still counts toward the day's alignment tally.
            assertThat(summary.counts().inconclusive()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("the list endpoint")
    class ListEndpoint {

        @Test
        @DisplayName("joins company names and converts the stored ratio to a percentage")
        void mapsRows() {
            portfolioExists();
            Security nvda = new Security();
            nvda.setSymbol("NVDA");
            nvda.setCompanyName("NVIDIA Corporation");
            when(assessments.findByPortfolioIdAndAttributionDateOrderByComputedAtDesc(
                    eq(PORTFOLIO_ID), eq(SESSION), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(row("NVDA", Alignment.CONFIRMED, "1614.36", "0.303"))));
            when(securities.findBySymbolIn(any())).thenReturn(List.of(nvda));

            PagedResponse<ImpactRow> paged =
                    service.list(PORTFOLIO_ID, SESSION, null, 1, 20);

            ImpactRow first = paged.content().get(0);
            assertThat(first.companyName()).isEqualTo("NVIDIA Corporation");
            // 0.0415 stored as a ratio becomes 4.15%.
            assertThat(first.priceChangePct()).isEqualTo(4.15);
            assertThat(first.valueImpact()).isEqualByComparingTo("1614.36");
            // 1-based page numbering at the boundary.
            assertThat(paged.page()).isEqualTo(1);
        }

        @Test
        @DisplayName("an unknown symbol falls back to the symbol as its name")
        void missingNameFallsBack() {
            portfolioExists();
            when(assessments.findByPortfolioIdAndAttributionDateOrderByComputedAtDesc(
                    eq(PORTFOLIO_ID), eq(SESSION), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(row("XYZQ", Alignment.CONFIRMED, "10.00", "0.01"))));
            when(securities.findBySymbolIn(any())).thenReturn(List.of());

            assertThat(service.list(PORTFOLIO_ID, SESSION, null, 1, 20)
                    .content().get(0).companyName()).isEqualTo("XYZQ");
        }

        @Test
        @DisplayName("an alignment filter uses the narrowed query")
        void filtersByAlignment() {
            portfolioExists();
            when(assessments
                    .findByPortfolioIdAndAttributionDateAndAlignmentOrderByComputedAtDesc(
                            eq(PORTFOLIO_ID), eq(SESSION), eq(Alignment.DIVERGENT),
                            any(Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(row("TSLA", Alignment.DIVERGENT, "-892.10", "0.20"))));

            PagedResponse<ImpactRow> paged =
                    service.list(PORTFOLIO_ID, SESSION, Alignment.DIVERGENT, 1, 20);

            assertThat(paged.content()).hasSize(1);
            assertThat(paged.content().get(0).alignment()).isEqualTo(Alignment.DIVERGENT);
        }
    }

    @Nested
    @DisplayName("a missing portfolio is a 404, not an empty result")
    class MissingPortfolio {

        @Test
        @DisplayName("the list endpoint rejects an unknown portfolio")
        void listRejects() {
            when(portfolios.existsById(anyLong())).thenReturn(false);

            // An empty page would imply the portfolio exists and had a quiet day.
            assertThatThrownBy(() -> service.list(404L, SESSION, null, 1, 20))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        @Test
        @DisplayName("the summary endpoint rejects an unknown portfolio")
        void summaryRejects() {
            when(portfolios.existsById(anyLong())).thenReturn(false);

            assertThatThrownBy(() -> service.summary(404L, SESSION))
                    .isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code())
                    .isEqualTo(ErrorCode.PORTFOLIO_NOT_FOUND);
        }
    }
}
