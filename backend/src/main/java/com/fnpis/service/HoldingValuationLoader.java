package com.fnpis.service;

import com.fnpis.domain.Holding;
import com.fnpis.domain.PriceQuote;
import com.fnpis.domain.Security;
import com.fnpis.repository.HoldingRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SecurityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Loads what a valuation needs and hands it to {@link ValuationService}.
 *
 * <p>Exists so the summary, list and holdings endpoints all value a portfolio
 * the same way. Two implementations of this would eventually disagree, and the
 * numbers disagreeing across screens is exactly what SC-002 catches.
 *
 * <p>Three queries regardless of position count - holdings, then quotes and
 * names by symbol set. Looking either up per row would be an N+1.
 */
@Component
class HoldingValuationLoader {

    private final HoldingRepository holdings;
    private final PriceQuoteRepository quotes;
    private final SecurityRepository securities;
    private final ValuationService valuation;

    HoldingValuationLoader(
            HoldingRepository holdings,
            PriceQuoteRepository quotes,
            SecurityRepository securities,
            ValuationService valuation) {
        this.holdings = holdings;
        this.quotes = quotes;
        this.securities = securities;
        this.valuation = valuation;
    }

    /** Values every position in one portfolio. Empty portfolios return zero totals (EC-01). */
    Valuation value(Long portfolioId, Instant now) {
        return value(holdings.findByPortfolioIdOrderBySymbol(portfolioId), now);
    }

    /**
     * Values a given set of positions.
     *
     * <p>Used by the paged holdings endpoint, where weights still have to be
     * relative to the <b>whole</b> portfolio rather than the current page - a
     * page-relative weight would sum to 100% on every page and mean nothing.
     */
    Valuation value(List<Holding> positions, Instant now) {
        if (positions.isEmpty()) {
            return valuation.value(positions, Map.of(), Map.of(), now);
        }
        Set<String> symbols = positions.stream()
                .map(Holding::getSymbol)
                .collect(Collectors.toSet());

        Map<String, PriceQuote> quotesBySymbol = quotes.findBySymbolIn(symbols).stream()
                .collect(Collectors.toMap(PriceQuote::getSymbol, Function.identity()));
        Map<String, String> namesBySymbol = securities.findBySymbolIn(symbols).stream()
                .collect(Collectors.toMap(Security::getSymbol, Security::getCompanyName));

        return valuation.value(positions, quotesBySymbol, namesBySymbol, now);
    }
}
