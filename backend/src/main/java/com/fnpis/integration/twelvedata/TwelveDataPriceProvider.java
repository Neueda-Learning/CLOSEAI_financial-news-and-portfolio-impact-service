package com.fnpis.integration.twelvedata;

import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
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
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "Twelve Data daily bars not implemented yet — see Module B5");
    }

}
