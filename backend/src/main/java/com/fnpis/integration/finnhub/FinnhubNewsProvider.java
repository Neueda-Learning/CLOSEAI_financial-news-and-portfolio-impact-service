package com.fnpis.integration.finnhub;

import com.fnpis.integration.NewsItem;
import com.fnpis.integration.NewsProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Qualifier("finnhubNewsProvider")
class FinnhubNewsProvider implements NewsProvider {

    private static final Logger log = LoggerFactory.getLogger(FinnhubNewsProvider.class);

    /** V2 column widths — keep in sync with the migration scripts. */
    private static final int HEADLINE_MAX = 512;
    private static final int SOURCE_MAX = 64;
    private static final int URL_MAX = 1024;

    private final RestClient restClient;
    private final String apiKey;

    FinnhubNewsProvider(
            @Qualifier("finnhubRestClient") RestClient restClient,
            @Value("${finnhub.keys.news}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    @CircuitBreaker(name = "finnhub")
    @RateLimiter(name = "finnhubNews")
    @Retry(name = "externalApi")
    public List<NewsItem> fetchCompanyNews(String symbol, LocalDate from, LocalDate to) {
        try {
            FinnhubNewsResponse[] responses = restClient.get()
                    .uri("/company-news?symbol={symbol}&from={from}&to={to}&token={token}",
                            symbol, from.toString(), to.toString(), apiKey)
                    .retrieve()
                    .body(FinnhubNewsResponse[].class);
            if (responses == null || responses.length == 0) {
                return Collections.emptyList();
            }
            return Arrays.stream(responses)
                    .filter(r -> r.headline() != null && !r.headline().isBlank()
                            && r.datetime() > 0)  // EC-14: skip if missing headline or time
                    .map(r -> new NewsItem(
                            String.valueOf(r.id()),
                            truncate(r.headline(), HEADLINE_MAX),
                            truncate(r.source(), SOURCE_MAX),
                            truncateUrl(r.url()),
                            r.summary(),
                            r.image() != null ? r.image() : "",
                            Instant.ofEpochSecond(r.datetime())))
                    .toList();
        } catch (Exception e) {
            log.warn("Finnhub news failed for {}: {}", symbol, e.getClass().getSimpleName());
            throw e;
        }
    }

    private String truncateUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.length() > URL_MAX ? url.substring(0, URL_MAX) : url;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
