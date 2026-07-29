package com.fnpis.integration;

import java.time.LocalDate;
import java.util.List;

/**
 * Fetch news from an external source (Finnhub).
 *
 * <p>Services depend on this interface, never on an implementation.
 * Only Finnhub provides news in the free tier; neither Twelve Data nor
 * yfinance have a news API. Fallback is implicit: when the provider is
 * unavailable, the DB still holds every story ever fetched, so the read
 * path degrades to slightly stale data instead of an error.
 */
public interface NewsProvider {

    /**
     * Company news for one symbol over a date range.
     *
     * @return empty list when no news exists for the range (not an error)
     */
    List<NewsItem> fetchCompanyNews(String symbol, LocalDate from, LocalDate to);
}
