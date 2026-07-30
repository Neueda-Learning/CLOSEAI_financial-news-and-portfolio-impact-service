package com.fnpis.integration.finnhub;

import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
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
@Qualifier("finnhubPriceProvider")
public class FinnhubPriceProvider implements PriceProvider {

    private static final Logger log = LoggerFactory.getLogger(FinnhubPriceProvider.class);
    private final RestClient restClient;
    private final String apiKey;

    FinnhubPriceProvider(
            @Qualifier("finnhubRestClient") RestClient restClient,
            @Value("${finnhub.keys.price}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    @CircuitBreaker(name = "finnhub")
    @RateLimiter(name = "finnhubPrice")
    @Retry(name = "externalApi")
    public Optional<QuoteSnapshot> fetchQuote(String symbol) {
        try {
            FinnhubQuoteResponse r = restClient.get()
                    .uri("/quote?symbol={symbol}&token={token}", symbol, apiKey)
                    .retrieve()
                    .body(FinnhubQuoteResponse.class);
            if (r == null || r.c() == null || r.c().signum() <= 0) {
                log.info("Finnhub returned no valid price for {}, likely halted or unknown", symbol);
                return Optional.empty();
            }
            Instant capturedAt = r.t() > 0 ? Instant.ofEpochSecond(r.t()) : null;
            return Optional.of(new QuoteSnapshot(
                    symbol,
                    r.c(),
                    r.pc() != null && r.pc().compareTo(BigDecimal.ZERO) > 0 ? r.pc() : null,
                    capturedAt));
        } catch (Exception e) {
            log.warn("Finnhub quote failed for {}: {}", symbol, e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "Finnhub daily bars not implemented yet — see Module B5");
    }
}
