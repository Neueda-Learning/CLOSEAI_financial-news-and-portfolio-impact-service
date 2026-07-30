package com.fnpis.api.internal;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.api.internal.dto.ImpactRow;
import com.fnpis.api.internal.dto.ImpactSummaryResponse;
import com.fnpis.api.internal.dto.ImpactViewResponse;
import com.fnpis.api.internal.dto.TopImpactedItem;
import com.fnpis.common.PagedResponse;
import com.fnpis.config.JacksonConfig;
import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import com.fnpis.domain.SentimentLabel;
import com.fnpis.service.ImpactQueryService;
import com.fnpis.service.ImpactViewService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The wire types of the three impact endpoints, asserted on the JSON itself.
 *
 * <p>Contract 1.2 splits the numeric types by meaning, not by convenience: money
 * and quantities are <b>strings</b> so JavaScript's double cannot round a figure a
 * lecturer is checking by hand (SC-002), while ratios, weights, percentages and
 * sentiment scores are <b>numbers</b> because the frontend multiplies and compares
 * them. {@code JacksonConfig} implements that split by quoting every
 * {@code BigDecimal}, which makes a DTO's declared type the wire contract.
 *
 * <p><b>Why this class exists.</b> Every other test in this package asserts on Java
 * objects, so a field typed {@code BigDecimal} where the contract wants a number
 * passes all of them and still breaks the frontend: {@code weight * 100} on
 * {@code "0.303000"} is string concatenation or NaN, and neither shows up as an
 * exception anywhere. That is exactly the defect this suite missed once. The
 * quoting assertions below are the point of the class - not the values.
 *
 * <p>{@code @WebMvcTest} loads the web layer only, so these run without a
 * database. {@code JacksonConfig} is imported explicitly because the slice does
 * <b>not</b> pick up a plain {@code @Configuration}: without it the ObjectMapper
 * is the stock one, money serialises unquoted, and this class would assert the
 * opposite of production behaviour while looking green.
 */
@WebMvcTest({ImpactController.class, ImpactViewController.class})
@Import(JacksonConfig.class)
class ImpactJsonShapeTest {

    private static final long PORTFOLIO_ID = 1L;
    private static final long ARTICLE_ID = 8842L;
    private static final LocalDate SESSION = LocalDate.of(2026, 7, 27);
    private static final Instant AS_OF = Instant.parse("2026-07-27T16:05:00Z");

    @Autowired private MockMvc mvc;

    @MockBean private ImpactQueryService queries;
    @MockBean private ImpactViewService views;

    /** The contract's own example row (contract 4.2), field for field. */
    private static ImpactRow row() {
        return new ImpactRow(
                "NVDA",
                "NVIDIA Corporation",
                0.303,
                4.15,
                0.192,
                1.258,
                new BigDecimal("1614.36"),
                Direction.POSITIVE,
                Alignment.CONFIRMED);
    }

    @Nested
    @DisplayName("GET /portfolios/{id}/impacts")
    class Impacts {

        private static final String PATH = "/api/v1/portfolios/" + PORTFOLIO_ID + "/impacts";

        @Test
        @DisplayName("weights and ratios are JSON numbers, money is a JSON string")
        void numericSplitHolds() throws Exception {
            given(queries.list(any(), any(), any(), any(Integer.class), any()))
                    .willReturn(new PagedResponse<>(
                            List.of(row()), 1, 20, 1, 1, AS_OF, false));

            mvc.perform(get(PATH).param("date", "2026-07-27"))
                    .andExpect(status().isOk())
                    // Numbers: the frontend does arithmetic on these.
                    .andExpect(jsonPath("$.content[0].holdingWeight").value(0.303))
                    .andExpect(jsonPath("$.content[0].priceChangePct").value(4.15))
                    .andExpect(jsonPath("$.content[0].expectedImpact").value(0.192))
                    .andExpect(jsonPath("$.content[0].observedContribution").value(1.258))
                    // The money figure, and only it, arrives quoted.
                    .andExpect(jsonPath("$.content[0].valueImpact").value("1614.36"))
                    // value() coerces, so assert on the raw text too: this is the
                    // only assertion that can tell 0.303 from "0.303".
                    .andExpect(content().string(
                            containsString("\"holdingWeight\":0.303")))
                    .andExpect(content().string(
                            containsString("\"expectedImpact\":0.192")))
                    .andExpect(content().string(
                            containsString(
                                    "\"observedContribution\":1.258")))
                    .andExpect(content().string(
                            containsString("\"valueImpact\":\"1614.36\"")));
        }

        @Test
        @DisplayName("an unmeasurable session leaves nulls, not zeros")
        void nullsSurvive() throws Exception {
            // EC-18: no previous close, so no return and no contribution. Zero
            // here would read as a flat session that was in fact never measured.
            ImpactRow unmeasured = new ImpactRow(
                    "NVDA", "NVIDIA Corporation", 0.303, null, 0.192, null, null,
                    Direction.POSITIVE, Alignment.INCONCLUSIVE);
            given(queries.list(any(), any(), any(), any(Integer.class), any()))
                    .willReturn(new PagedResponse<>(
                            List.of(unmeasured), 1, 20, 1, 1, AS_OF, false));

            mvc.perform(get(PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].priceChangePct").doesNotExist())
                    .andExpect(jsonPath("$.content[0].observedContribution").doesNotExist())
                    .andExpect(jsonPath("$.content[0].valueImpact").doesNotExist())
                    .andExpect(jsonPath("$.content[0].alignment").value("INCONCLUSIVE"));
        }
    }

    @Nested
    @DisplayName("GET /portfolios/{id}/impact-summary")
    class Summary {

        private static final String PATH =
                "/api/v1/portfolios/" + PORTFOLIO_ID + "/impact-summary";

        @Test
        @DisplayName("the three rates are numbers and topImpacted's money is a string")
        void ratesAreNumbers() throws Exception {
            given(queries.summary(any(), any()))
                    .willReturn(new ImpactSummaryResponse(
                            SESSION, 0.34, 0.60, 0.71, 24,
                            new ImpactSummaryResponse.AlignmentCounts(5, 2, 4),
                            List.of(new TopImpactedItem(
                                    "NVDA", new BigDecimal("1614.36"), Alignment.CONFIRMED)),
                            AS_OF, false));

            mvc.perform(get(PATH))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            containsString(
                                    "\"directionAgreementRate\":0.71")))
                    .andExpect(content().string(
                            containsString("\"weightedSentiment\":0.34")))
                    // SC-008: the rate is meaningless without the sample it rests on.
                    .andExpect(jsonPath("$.sampleSize").value(24))
                    .andExpect(content().string(
                            containsString("\"valueImpact\":\"1614.36\"")));
        }

        @Test
        @DisplayName("SC-008: too small a sample publishes a null rate, not a misleading one")
        void thinSampleHasNoRate() throws Exception {
            given(queries.summary(any(), any()))
                    .willReturn(new ImpactSummaryResponse(
                            SESSION, null, 0.0, null, 3,
                            new ImpactSummaryResponse.AlignmentCounts(2, 1, 0),
                            List.of(), AS_OF, true));

            mvc.perform(get(PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.directionAgreementRate").doesNotExist())
                    // The sample size still ships, so the frontend can say why.
                    .andExpect(jsonPath("$.sampleSize").value(3))
                    .andExpect(jsonPath("$.stale").value(true));
        }
    }

    @Nested
    @DisplayName("GET /news/{id}/impact-view")
    class View {

        private static final String PATH = "/api/v1/news/" + ARTICLE_ID + "/impact-view";

        private static ImpactViewResponse response() {
            return new ImpactViewResponse(
                    new ImpactViewResponse.Article(
                            ARTICLE_ID,
                            "Nvidia beats Q2 estimates, raises guidance",
                            "Reuters",
                            "https://example.com/article/8842",
                            Instant.parse("2026-07-27T12:31:00Z"),
                            new ImpactViewResponse.Sentiment(
                                    SentimentLabel.POSITIVE, 0.72, 0.88, "agent-v1")),
                    SESSION,
                    List.of("NVDA", "AMD"),
                    "NVDA",
                    List.of(row()),
                    new ImpactViewResponse.PriceSeries(
                            "NVDA",
                            new BigDecimal("121.40"),
                            Instant.parse("2026-07-27T12:35:00Z"),
                            List.of(new ImpactViewResponse.PriceSeries.Point(
                                    Instant.parse("2026-07-27T12:35:00Z"),
                                    new BigDecimal("125.60")))),
                    AS_OF,
                    false);
        }

        @Test
        @DisplayName("the sentiment score is a number; prices stay strings")
        void sentimentIsANumber() throws Exception {
            given(views.view(any(), any(), any(, false, false))).willReturn(response());

            mvc.perform(get(PATH).param("portfolioId", String.valueOf(PORTFOLIO_ID)))
                    .andExpect(status().isOk())
                    // Contract 1.2 lists a sentiment score as a number in -1..1.
                    // Quoted, the frontend's threshold comparison becomes a string
                    // compare and the label colour goes wrong silently.
                    .andExpect(content().string(
                            containsString("\"score\":0.72")))
                    .andExpect(content().string(
                            containsString("\"confidence\":0.88")))
                    // Prices are money: quoted, per the contract's own example.
                    .andExpect(content().string(
                            containsString("\"previousClose\":\"121.40\"")))
                    .andExpect(content().string(
                            containsString("\"price\":\"125.60\"")));
        }

        @Test
        @DisplayName("the curve is captioned by selectedSymbol, and the two agree")
        void curveIsBound() throws Exception {
            given(views.view(any(), any(), any(, false, false))).willReturn(response());

            mvc.perform(get(PATH).param("portfolioId", String.valueOf(PORTFOLIO_ID)))
                    .andExpect(status().isOk())
                    // The documented trap: reading the curve's owner off impacts[0]
                    // instead of this field.
                    .andExpect(jsonPath("$.selectedSymbol").value("NVDA"))
                    .andExpect(jsonPath("$.priceSeries.symbol").value("NVDA"))
                    .andExpect(jsonPath("$.impactedSymbols[0]").value("NVDA"))
                    .andExpect(jsonPath("$.impactedSymbols[1]").value("AMD"))
                    // The marker is on the series' own axis, not publishedAt.
                    .andExpect(jsonPath("$.priceSeries.newsMarker")
                            .value("2026-07-27T12:35:00Z"))
                    .andExpect(jsonPath("$.article.publishedAt")
                            .value("2026-07-27T12:31:00Z"));
        }

        @Test
        @DisplayName("portfolioId is required - a value impact has no meaning without it")
        void portfolioIdRequired() throws Exception {
            mvc.perform(get(PATH)).andExpect(status().isBadRequest());
        }
    }
}

