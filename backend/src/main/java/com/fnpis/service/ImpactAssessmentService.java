package com.fnpis.service;

import com.fnpis.common.error.ApiException;
import com.fnpis.domain.ArticleSecurityLink;
import com.fnpis.domain.ImpactAssessment;
import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.PriceBar;
import com.fnpis.domain.PriceQuote;
import com.fnpis.domain.SentimentScore;
import com.fnpis.repository.ArticleSecurityLinkRepository;
import com.fnpis.repository.ImpactAssessmentRepository;
import com.fnpis.repository.NewsArticleRepository;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.PriceBarRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SentimentScoreRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the four inputs the impact formula needs, runs {@link ImpactEngine},
 * and persists one row per (article, symbol, portfolio) for a session
 * (requirements 5.3, E1-E4). The project's core write path.
 *
 * <p><b>Persisted, not computed on read (decision 3).</b> The read endpoints and
 * the linked view serve {@code impact_assessment} rows; the numbers depend on
 * prices at a point in time, so recomputing on read would answer differently
 * every call and nothing would be reproducible on demo day.
 *
 * <p><b>The engine stays pure; this service is where the data comes from.</b>
 * {@link ImpactEngine} takes an {@link ImpactInput} and holds no repository, so
 * the three worked examples from 5.3 run as plain unit tests (SC-012). Every
 * lookup and every skip decision lives here instead.
 *
 * <p>{@link #recomputeFor(LocalDate)} takes no HTTP type on purpose. This round
 * it is driven by {@code POST /api/v1/impacts/recompute} on an HTTP thread, but
 * a {@code @Scheduled} job with an execution lock can call the same method later
 * without reshaping it (dev plan 3.7).
 */
@Service
public class ImpactAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(ImpactAssessmentService.class);

    /** Scale for r, matching {@code price_change_ratio DECIMAL(10,6)}. */
    private static final int RATIO_SCALE = 6;

    private final NewsArticleRepository articles;
    private final ArticleSecurityLinkRepository links;
    private final SentimentScoreRepository sentiments;
    private final PriceBarRepository bars;
    private final PriceQuoteRepository quotes;
    private final ImpactAssessmentRepository assessments;
    private final PortfolioRepository portfolios;
    private final PortfolioService portfolioService;
    private final AttributionDateResolver attributionDates;
    private final ImpactEngine engine;

    /**
     * Moves smaller than this are noise, not a reaction (requirements 5.3).
     * Injected rather than hard-coded so the value the engine sees and the value
     * the config documents are the same one.
     */
    private final BigDecimal epsilon;

    /** Guards against two runs upserting the same session's rows at once. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    ImpactAssessmentService(
            NewsArticleRepository articles,
            ArticleSecurityLinkRepository links,
            SentimentScoreRepository sentiments,
            PriceBarRepository bars,
            PriceQuoteRepository quotes,
            ImpactAssessmentRepository assessments,
            PortfolioRepository portfolios,
            PortfolioService portfolioService,
            AttributionDateResolver attributionDates,
            ImpactEngine engine,
            @Value("${app.impact.epsilon}") BigDecimal epsilon) {
        this.articles = articles;
        this.links = links;
        this.sentiments = sentiments;
        this.bars = bars;
        this.quotes = quotes;
        this.assessments = assessments;
        this.portfolios = portfolios;
        this.portfolioService = portfolioService;
        this.attributionDates = attributionDates;
        this.engine = engine;
        this.epsilon = epsilon;
    }

    /**
     * Claims the right to run, as module BC's poll does.
     *
     * <p>The lock lives on the service rather than on the scheduler so the timer
     * and the admin endpoint contend for one lock instead of two. It has to exist
     * even though the run is transactional: two concurrent recomputes of the same
     * session would each upsert the same rows, and the loser's numbers would
     * silently replace the winner's rather than colliding on the unique
     * constraint (EC-20, EC-23).
     */
    public boolean tryAcquire() {
        return running.compareAndSet(false, true);
    }

    public void release() {
        running.set(false);
    }

    /**
     * Recomputes one portfolio's impacts for one session.
     *
     * <p>The admin endpoint's narrow path. Kept separate from the all-portfolios
     * run rather than folded in behind a nullable id, so neither caller can reach
     * the other's scope by passing the wrong thing.
     *
     * @throws ApiException 404 when the portfolio does not exist - a silent zero
     *         would read as "recomputed, nothing to do" for a typo'd id
     */
    @Transactional
    public int recomputeOne(Long portfolioId, LocalDate session, Instant now) {
        if (!portfolios.existsById(portfolioId)) {
            throw ApiException.portfolioNotFound(portfolioId);
        }
        int written = recomputePortfolio(portfolioId, session, sessionArticles(session, now), now);
        log.info("Impact recompute for portfolio {} on {} complete: {} rows",
                portfolioId, session, written);
        return written;
    }

    /**
     * Recomputes every impact attributed to {@code session} across every
     * portfolio.
     *
     * <p>Idempotent: a re-run overwrites the day's rows in place rather than
     * duplicating them (the {@code uq_impact_article_symbol_portfolio_date}
     * constraint plus the upsert in {@link #persist}). Safe to trigger twice on
     * demo day.
     *
     * @param now the clock reference for valuation and for the "is d today" test
     *            in tier 2 of the return derivation; a parameter so tests pin it
     * @return how many assessment rows were written or updated
     */
    @Transactional
    public int recomputeFor(Instant now) {
        LocalDate session = attributionDates.resolve(now);
        return recomputeFor(session, now);
    }

    /**
     * Recomputes the impacts for one explicit session. Separated from
     * {@link #recomputeFor(Instant)} so a caller that already knows the session
     * (a backfill, a test) does not have it re-derived from the clock.
     */
    @Transactional
    public int recomputeFor(LocalDate session, Instant now) {
        // Snapshot the portfolio ids up front. A portfolio deleted mid-loop makes
        // PortfolioService.weights() throw a 404 (A's shared require()); catching
        // per-portfolio below keeps one deletion from aborting the whole run.
        List<Long> portfolioIds = portfolios.findAll().stream()
                .map(p -> p.getId())
                .toList();
        if (portfolioIds.isEmpty()) {
            return 0;
        }

        List<NewsArticle> sessionArticles = sessionArticles(session, now);

        int written = 0;
        for (Long portfolioId : portfolioIds) {
            written += recomputePortfolio(portfolioId, session, sessionArticles, now);
        }
        log.info("Impact recompute for {} complete: {} rows across {} portfolios",
                session, written, portfolioIds.size());
        return written;
    }

    /**
     * Every story charged to {@code session}.
     *
     * <p>The attribution date is derived rather than stored, so the filter runs in
     * memory: the window is a few days of articles, small enough that the extra
     * rows cost less than a second round-trip would. The window opens before the
     * session because a Friday-evening story is charged to Monday.
     */
    private List<NewsArticle> sessionArticles(LocalDate session, Instant now) {
        return articles.findByPublishedAtBetween(windowStart(session), windowEnd(now)).stream()
                .filter(a -> attributionDates.resolve(a.getPublishedAt()).equals(session))
                .toList();
    }

    private int recomputePortfolio(
            Long portfolioId, LocalDate session, List<NewsArticle> sessionArticles, Instant now) {
        PortfolioWeights weights;
        try {
            weights = portfolioService.weights(portfolioId, now);
        } catch (ApiException deletedMidRun) {
            // The portfolio went away between the snapshot and here. Nothing to
            // assess against it; the next run will not see it at all.
            log.debug("Portfolio {} vanished during recompute, skipping", portfolioId);
            return 0;
        }

        int written = 0;
        for (NewsArticle article : sessionArticles) {
            // Step 2: no sentiment yet means the analysis job has not reached this
            // headline. Skip - a fabricated neutral verdict would pollute E5's
            // direction-agreement rate.
            Optional<SentimentScore> sentiment = sentiments.findByArticleId(article.getId());
            if (sentiment.isEmpty()) {
                continue;
            }
            SentimentScore s = sentiment.get();

            // Step 1: article x holding, intersected on symbol. A symbol the
            // portfolio does not hold produces no PositionWeight and is skipped
            // (EC-15); a story touching several held symbols writes one row each
            // (EC-24). Both fall out of the intersection with no special code.
            for (ArticleSecurityLink link : links.findByArticleId(article.getId())) {
                String symbol = link.getSymbol();
                PositionWeight position = weights.of(symbol).orElse(null);
                if (position == null || !position.assessable()) {
                    // Not held, or held with no quote (EC-13). holding_weight is
                    // NOT NULL and a zero would read as "no impact" rather than
                    // "cannot value", so no row is written (module A doc 4.2).
                    continue;
                }

                BigDecimal ratio = returnFor(symbol, session, now);
                ImpactOutput out = engine.assess(new ImpactInput(
                        s.getScore(), s.getConfidence(),
                        position.weight(), ratio, position.marketValue(), epsilon));

                persist(article.getId(), symbol, portfolioId, session,
                        position.weight(), ratio, out);
                written++;
            }
        }
        return written;
    }

    /**
     * Session return r, two tiers (dev plan 5.3 step 5).
     *
     * <ol>
     *   <li>{@code price_bar} has d and its prior session: the real answer.</li>
     *   <li>Else, only when d is today: derive from the live quote's price and
     *       previous close - the only source until B5's snapshot job fills
     *       {@code price_bar} (architecture 6.3).</li>
     *   <li>Else null: no prior close, so INCONCLUSIVE (EC-18), never a zero.</li>
     * </ol>
     *
     * @return r as a ratio at 6 decimals, or null when neither tier can supply it
     */
    private BigDecimal returnFor(String symbol, LocalDate session, Instant now) {
        // Tier 1: two most recent bars up to d. The query decides what "prior
        // session" means, so a long weekend or holiday needs no calendar here.
        List<PriceBar> recent =
                bars.findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(symbol, session);
        if (recent.size() == 2 && recent.get(0).getTradeDate().equals(session)) {
            return ratio(recent.get(0).getClosePrice(), recent.get(1).getClosePrice());
        }

        // Tier 2: only valid when the story's session is today - price_quote holds
        // one row per symbol, overwritten each refresh, so it cannot answer for a
        // past day.
        if (session.equals(attributionDates.resolve(now))) {
            PriceQuote quote = quotes.findById(symbol).orElse(null);
            if (quote != null && isPositive(quote.getPrice()) && isPositive(quote.getPreviousClose())) {
                // Guard signum, not just null: BC's FinnhubPriceProvider only
                // rejects a null previous close, so a 0.0000 can reach here and a
                // null-only check would divide by zero (dev plan 5.3 step 5).
                return ratio(quote.getPrice(), quote.getPreviousClose());
            }
        }

        // Tier 3: EC-18. The engine turns a null r into INCONCLUSIVE.
        return null;
    }

    /** (current - prior) / prior, at ratio scale. Prior is guaranteed non-zero by the callers. */
    private BigDecimal ratio(BigDecimal current, BigDecimal prior) {
        return current.subtract(prior).divide(prior, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * Writes the assessment, overwriting the existing row for the same
     * (article, symbol, portfolio, session) if one is there.
     *
     * <p>Update-in-place rather than delete-then-insert: a recompute that fails
     * halfway must not leave a hole in the day's data, and the unique constraint
     * makes at most one row to find (V4 migration comment).
     */
    private void persist(
            Long articleId, String symbol, Long portfolioId, LocalDate session,
            BigDecimal weight, BigDecimal ratio, ImpactOutput out) {
        ImpactAssessment row = assessments
                .findByArticleIdAndSymbolAndPortfolioIdAndAttributionDate(
                        articleId, symbol, portfolioId, session)
                .orElseGet(ImpactAssessment::new);

        row.setArticleId(articleId);
        row.setSymbol(symbol);
        row.setPortfolioId(portfolioId);
        row.setAttributionDate(session);
        row.setHoldingWeight(weight);
        // r as the engine saw it: null flows straight through to an INCONCLUSIVE
        // row (EC-18), and storing it here rather than reconstructing it from
        // observedContribution avoids a divide that would reintroduce rounding.
        row.setPriceChangeRatio(ratio == null ? null
                : ratio.setScale(RATIO_SCALE, RoundingMode.HALF_UP));
        row.setExpectedImpact(out.expectedImpact());
        row.setObservedContribution(out.observedContribution());
        row.setValueImpact(out.valueImpact());
        row.setDirection(out.direction());
        row.setAlignment(out.alignment());
        // computed_at is stamped by @PrePersist on insert; on update it keeps the
        // original, which is wrong - refresh it so it answers "when concluded".
        row.setComputedAt(Instant.now());

        assessments.save(row);
    }

    /**
     * The article window's lower bound. A story charged to {@code session} could
     * have been published up to three days earlier (a Friday-evening story lands
     * on Monday), so the window opens a few days before the session to be safe;
     * the exact attribution is re-checked per article in memory.
     */
    private static Instant windowStart(LocalDate session) {
        return session.minusDays(4).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    }

    private static Instant windowEnd(Instant now) {
        return now;
    }
}
