package com.fnpis.integration.mock;

import com.fnpis.integration.NewsItem;
import com.fnpis.integration.NewsProvider;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Offline news provider for zero-config startup and E2E tests.
 *
 * <p>Activate by setting {@code PROVIDER_NEWS=mock} in {@code .env}.
 * Returns empty news so the service starts without real API keys.
 * Real preset data is tracked separately; until it lands, manual
 * news refresh via {@code POST /api/v1/news/refresh} will report
 * 0 inserted articles without error.
 *
 * <p>Mutually exclusive with {@link com.fnpis.integration.finnhub.FinnhubNewsProvider}
 * via {@code @ConditionalOnProperty}.
 */
@Component
@ConditionalOnProperty(name = "app.providers.news", havingValue = "mock")
public class MockNewsProvider implements NewsProvider {

    @Override
    public List<NewsItem> fetchCompanyNews(String symbol, LocalDate from, LocalDate to) {
        return Collections.emptyList();
    }
}
