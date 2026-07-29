package com.fnpis.service;

import com.fnpis.api.internal.dto.ImpactRow;
import com.fnpis.api.internal.dto.ImpactSummaryResponse;
import com.fnpis.api.internal.dto.TopImpactedItem;
import com.fnpis.common.Freshness;
import com.fnpis.common.PagedResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.domain.Alignment;
import com.fnpis.domain.Holding;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.Security;
import com.fnpis.repository.HoldingRepository;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.SecurityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The read path for assessed impacts (E1-E3, E5).
 *
 * <p>Serves persisted {@code impact_assessment} rows and never computes an
 * assessment. That is decision 3: the numbers a client sees are the ones the
 * recompute job wrote, so two requests for the same session agree with each
 * other and with what the demo showed a minute ago. {@link ImpactEngine} is not
 * reachable from here on purpose.
 *
 * <p>Reads touch no provider either (decision 2), so nothing on this path can
 * fail because an upstream is down - an empty session yields an empty answer,
 * not an error.
 */
@Service
public class ImpactQueryService {

    /**
     * Below this many confirmed-or-divergent rows, the agreement rate is
     * reported as null rather than as a number (SC-008). A rate off three rows
     * would swing on a single story and read as precision the data has not
     * earned.
     */
    private static final int MIN_SAMPLE_FOR_RATE = 20;

    /** How many movers the summary lists. */
    private static final int TOP_IMPACTED_LIMIT = 5;

    /** Percentages and rates: enough digits to be exact, few enough to read. */
    private static final int PCT_SCALE = 4;

    private final ImpactAssessmentRepository assessments;
    private final HoldingRepository holdings;
    private final SecurityRepository securities;
    private final PortfolioRepository portfolios;

    ImpactQueryService(
            ImpactAssessmentRepository assessments,
            HoldingRepository holdings,
            SecurityRepository securities,
            PortfolioRepository portfolios) {
        this.assessments = assessments;
        this.holdings = holdings;
        this.securities = securities;
        this.portfolios = portfolios;
    }

    /**
     * One session's impacts for a portfolio, paged and optionally filtered by
     * alignment (E1-E3).
     *
     * @param alignment null for all three
     * @throws ApiException 404 when the portfolio does not exist - distinct from
     *         a portfolio that exists but had no news, which is an empty page
     */
    @Transactional(readOnly = true)
    public PagedResponse<ImpactRow> list(
            Long portfolioId, LocalDate session, Alignment alignment, int page, Integer size) {
        requirePortfolio(portfolioId);

        Pageable request = PageRequest.of(page - 1, PagedResponse.clampSize(size));
        Page<ImpactAssessment> found = alignment == null
                ? assessments.findByPortfolioIdAndAttributionDateOrderByComputedAtDesc(
                        portfolioId, session, request)
                : assessments
                        .findByPortfolioIdAndAttributionDateAndAlignmentOrderByComputedAtDesc(
                                portfolioId, session, alignment, request);

        Map<String, String> names = companyNames(found.getContent());
        Page<ImpactRow> rows = found.map(row -> ImpactRowMapper.toRow(row, names));

        // The rows were computed by a job, so their age is the freshness that
        // matters here - not a quote's.
        return PagedResponse.from(rows, freshnessOf(found.getContent()));
    }

    /**
     * One session's roll-up: agreement rate, coverage and top movers (E5).
     *
     * <p>Reads the whole session rather than a page because every figure here is
     * an aggregate over it. A day holds at most one row per (story, symbol), so
     * this stays bounded by the watchlist size times the day's news volume.
     */
    @Transactional(readOnly = true)
    public ImpactSummaryResponse summary(Long portfolioId, LocalDate session) {
        requirePortfolio(portfolioId);

        List<ImpactAssessment> rows =
                assessments.findByPortfolioIdAndAttributionDate(portfolioId, session);

        int confirmed = countOf(rows, Alignment.CONFIRMED);
        int divergent = countOf(rows, Alignment.DIVERGENT);
        int inconclusive = countOf(rows, Alignment.INCONCLUSIVE);
        Freshness freshness = freshnessOf(rows);

        return new ImpactSummaryResponse(
                session,
                weightedSentiment(rows),
                newsCoverage(portfolioId, rows),
                agreementRate(confirmed, divergent),
                confirmed + divergent,
                new ImpactSummaryResponse.AlignmentCounts(confirmed, divergent, inconclusive),
                topImpacted(rows),
                freshness.asOf(),
                freshness.stale());
    }

    /**
     * Confirmed as a share of the rows that expressed an opinion either way.
     *
     * <p>INCONCLUSIVE rows are excluded from the denominator: a move too small to
     * read is not the price disagreeing with the news, and counting it as one
     * would drag the rate toward zero on a quiet day.
     *
     * @return null when the sample is below {@link #MIN_SAMPLE_FOR_RATE}, or when
     *         no row expressed an opinion at all
     */
    private Double agreementRate(int confirmed, int divergent) {
        int sample = confirmed + divergent;
        if (sample < MIN_SAMPLE_FOR_RATE) {
            return null;
        }
        return BigDecimal.valueOf(confirmed)
                .divide(BigDecimal.valueOf(sample), PCT_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Portfolio-weighted mean of the expected impacts.
     *
     * <p>{@code expectedImpact} is already weight times sentiment, so summing it
     * gives the portfolio-level directional signal directly. Dividing by total
     * weight rather than by row count keeps a large position's story from
     * counting the same as a tiny one's.
     *
     * @return null when the session is empty or every weight is zero (EC-22's
     *         shape: no denominator means no answer, not zero)
     */
    private Double weightedSentiment(List<ImpactAssessment> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        BigDecimal weightSum = BigDecimal.ZERO;
        BigDecimal impactSum = BigDecimal.ZERO;
        for (ImpactAssessment row : rows) {
            if (row.getHoldingWeight() == null || row.getExpectedImpact() == null) {
                continue;
            }
            weightSum = weightSum.add(row.getHoldingWeight());
            impactSum = impactSum.add(row.getExpectedImpact());
        }
        if (weightSum.signum() == 0) {
            return null;
        }
        return impactSum.divide(weightSum, PCT_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Share of held symbols that at least one story touched this session.
     *
     * @return null when the portfolio holds nothing - zero coverage of zero
     *         holdings is a division by zero, not a 0% result (EC-22)
     */
    private Double newsCoverage(Long portfolioId, List<ImpactAssessment> rows) {
        List<Holding> held = holdings.findByPortfolioIdOrderBySymbol(portfolioId);
        if (held.isEmpty()) {
            return null;
        }
        Set<String> covered = rows.stream()
                .map(ImpactAssessment::getSymbol)
                .collect(Collectors.toSet());
        long hit = held.stream().map(Holding::getSymbol).distinct().filter(covered::contains).count();
        return BigDecimal.valueOf(hit)
                .divide(BigDecimal.valueOf(held.size()), PCT_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * The session's biggest movers by absolute value impact, sign preserved.
     *
     * <p>Rows with no value impact (no return to multiply) sort last rather than
     * being dropped: they are real assessments, just not rankable ones.
     */
    private List<TopImpactedItem> topImpacted(List<ImpactAssessment> rows) {
        return rows.stream()
                .filter(row -> row.getValueImpact() != null)
                .sorted(Comparator.comparing(
                        (ImpactAssessment row) -> row.getValueImpact().abs()).reversed())
                .limit(TOP_IMPACTED_LIMIT)
                .map(row -> new TopImpactedItem(
                        row.getSymbol(), row.getValueImpact(), row.getAlignment()))
                .toList();
    }

    /** Symbol to company name for the rows on this page, in one query. */
    private Map<String, String> companyNames(List<ImpactAssessment> rows) {
        Set<String> symbols = rows.stream()
                .map(ImpactAssessment::getSymbol)
                .collect(Collectors.toSet());
        if (symbols.isEmpty()) {
            return Map.of();
        }
        Map<String, String> names = new HashMap<>();
        for (Security security : securities.findBySymbolIn(symbols)) {
            names.put(security.getSymbol(), security.getCompanyName());
        }
        return names;
    }

    /**
     * How old the assessments are, taken from the oldest row in the set.
     *
     * <p>The oldest rather than the newest because a mixed set is only as fresh
     * as its stalest member. An empty set has no capture time at all, which
     * {@link Freshness} represents as stale with a null {@code asOf} - honest
     * about there being nothing rather than claiming freshness.
     */
    private Freshness freshnessOf(List<ImpactAssessment> rows) {
        return rows.stream()
                .map(ImpactAssessment::getComputedAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .map(Freshness::fresh)
                .orElseGet(() -> Freshness.stale(null));
    }

    private int countOf(List<ImpactAssessment> rows, Alignment alignment) {
        return (int) rows.stream().filter(row -> alignment == row.getAlignment()).count();
    }

    private void requirePortfolio(Long portfolioId) {
        if (!portfolios.existsById(portfolioId)) {
            throw ApiException.portfolioNotFound(portfolioId);
        }
    }
}
