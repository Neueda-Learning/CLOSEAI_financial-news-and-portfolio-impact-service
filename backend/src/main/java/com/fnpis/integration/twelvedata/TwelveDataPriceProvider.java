package com.fnpis.integration.twelvedata;

import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Qualifier("twelveDataPriceProvider")
class TwelveDataPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataPriceProvider.class);
    private final RestClient restClient;
    private final String apiKey;

    TwelveDataPriceProvider(
            @Qualifier("twelvedataRestClient") RestClient restClient,
            @Value("${twelvedata.key:}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    @RateLimiter(name = "twelvedata")
    public Optional<QuoteSnapshot> fetchQuote(String symbol) {
        try {
            TwelveDataQuoteResponse r = restClient.get()
                    .uri("/quote?symbol={symbol}&apikey={apikey}", symbol, apiKey)
                    .retrieve()
                    .body(TwelveDataQuoteResponse.class);
            if (r == null || r.close() == null) {
                log.info("Twelve Data returned no data for {}", symbol);
                return Optional.empty();
            }
            return Optional.of(new QuoteSnapshot(
                    symbol,
                    r.close(),
                    r.previousClose() != null && r.previousClose().compareTo(BigDecimal.ZERO) > 0
                            ? r.previousClose() : null,
                    Instant.ofEpochSecond(r.timestamp())));
        } catch (Exception e) {
            log.warn("Twelve Data quote failed for {}: {}", symbol, e.getMessage());
            throw e;
        }
    }

    @Override
    @RateLimiter(name = "twelvedata")
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        int days = (int) from.until(to).getDays() + 1;
        try {
            TwelveDataTimeSeriesResponse r = restClient.get()
                    .uri("/time_series?symbol={symbol}&interval=1day&outputsize={size}&apikey={apikey}",
                            symbol, days, apiKey)
                    .retrieve()
                    .body(TwelveDataTimeSeriesResponse.class);
            if (r == null || r.values() == null) {
                return List.of();
            }
            return r.values().stream()
                    .filter(v -> {
                        LocalDate d = LocalDate.parse(v.datetime());
                        return !d.isBefore(from) && !d.isAfter(to);
                    })
                    .map(v -> new DailyBar(
                            symbol,
                            LocalDate.parse(v.datetime()),
                            v.open(), v.high(), v.low(), v.close(), v.volume()))
                    .toList();
        } catch (Exception e) {
            log.warn("Twelve Data daily bars failed for {}: {}", symbol, e.getMessage());
            throw e;
        }
    }

}
