package com.fnpis.api.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.api.internal.dto.HoldingRow;
import com.fnpis.api.internal.dto.PortfolioResponse;
import com.fnpis.api.internal.dto.PortfolioSummaryResponse;
import com.fnpis.common.Freshness;
import com.fnpis.common.PagedResponse;
import com.fnpis.config.JacksonConfig;
import com.fnpis.service.HoldingService;
import com.fnpis.service.PortfolioService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Success status codes for the nine module-A endpoints (doc 8 section 1).
 *
 * <p>Locks the two that are easy to "fix" into being wrong: {@code POST
 * /holdings} answers <b>200</b> because a repeat symbol merges and creates
 * nothing (EC-09), and both deletes answer <b>204</b> with an empty body. A
 * later refactor that returns 201 from the add would be a silent contract break
 * - the response body is unchanged, so only the status reveals it.
 *
 * <p>Money is asserted as a JSON string, not a number: {@code JacksonConfig}
 * serialises {@code BigDecimal} that way on purpose (architecture 6.2), and a
 * change to it would break every client's parsing.
 */
@WebMvcTest(controllers = {PortfolioController.class, HoldingController.class})
// The web slice does not pick up plain @Configuration beans, so without this the
// BigDecimal-as-string rule would be missing and the assertions below would be
// testing Jackson's default rather than our contract.
@Import(JacksonConfig.class)
@DisplayName("Success status codes (doc 8 section 1)")
class SuccessStatusTest {

    private static final String PORTFOLIOS = "/api/v1/portfolios";
    private static final Instant AS_OF = Instant.parse("2026-07-29T13:45:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private HoldingService holdingService;

    @Test
    @DisplayName("A1: creating a portfolio is 201")
    void createPortfolioIs201() throws Exception {
        given(portfolioService.create(any())).willReturn(portfolio());

        mvc.perform(post(PORTFOLIOS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                // Money crosses the boundary as a string (architecture 6.2).
                .andExpect(jsonPath("$.totalMarketValue").value("0.00"));
    }

    @Test
    @DisplayName("A2: listing portfolios is 200")
    void listPortfoliosIs200() throws Exception {
        given(portfolioService.list(any())).willReturn(List.of(portfolio()));

        mvc.perform(get(PORTFOLIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("A2/B2/B3: summary is 200")
    void summaryIs200() throws Exception {
        given(portfolioService.summary(eq(1L), any())).willReturn(summary());

        mvc.perform(get(PORTFOLIOS + "/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMarketValue").value("22560.00"));
    }

    @Test
    @DisplayName("A2: detail returns the same shape as summary")
    void detailMatchesSummary() throws Exception {
        given(portfolioService.summary(eq(1L), any())).willReturn(summary());

        mvc.perform(get(PORTFOLIOS + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMarketValue").value("22560.00"))
                .andExpect(jsonPath("$.allocations").isArray());
    }

    @Test
    @DisplayName("A3: deleting a portfolio is 204 with no body")
    void deletePortfolioIs204() throws Exception {
        mvc.perform(delete(PORTFOLIOS + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(portfolioService).delete(1L);
    }

    @Test
    @DisplayName("A5: listing holdings is 200 and paged")
    void listHoldingsIs200() throws Exception {
        given(holdingService.list(eq(1L), any(), any(), any())).willReturn(pageOfOne());

        mvc.perform(get(PORTFOLIOS + "/1/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                // 1-based page numbering (contract 1.4), converted in PagedResponse.from
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.asOf").exists());
    }

    @Test
    @DisplayName("A4/EC-09: adding a holding is 200, not 201 - a repeat add creates nothing")
    void addHoldingIs200NotCreated() throws Exception {
        given(holdingService.add(eq(1L), any(), any())).willReturn(holdingRow());

        mvc.perform(post(PORTFOLIOS + "/1/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"NVDA\",\"quantity\":100,\"costBasis\":\"140.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("NVDA"));
    }

    @Test
    @DisplayName("A7: patching a holding is 200")
    void updateHoldingIs200() throws Exception {
        given(holdingService.update(eq(1L), any(), any())).willReturn(holdingRow());

        mvc.perform(patch("/api/v1/holdings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costBasis").value("140.00"));
    }

    @Test
    @DisplayName("A6: deleting a holding is 204 with no body")
    void deleteHoldingIs204() throws Exception {
        mvc.perform(delete("/api/v1/holdings/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(holdingService).delete(1L);
    }

    @Test
    @DisplayName("Lowercase symbols are accepted; the service normalises them")
    void lowercaseSymbolAccepted() throws Exception {
        given(holdingService.add(eq(1L), any(), any())).willReturn(holdingRow());

        mvc.perform(post(PORTFOLIOS + "/1/holdings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"nvda\",\"quantity\":10,\"costBasis\":\"100.00\"}"))
                .andExpect(status().isOk());
    }

    private static PortfolioResponse portfolio() {
        return new PortfolioResponse(
                1L, "Test", "USD", new BigDecimal("0.00"), 0L, AS_OF, null, false);
    }

    private static PortfolioSummaryResponse summary() {
        return new PortfolioSummaryResponse(
                1L, "Test", "USD",
                new BigDecimal("22560.00"), new BigDecimal("23000.00"), new BigDecimal("-440.00"),
                -1.91, null, null,
                List.of(), AS_OF, false);
    }

    private static HoldingRow holdingRow() {
        return new HoldingRow(
                1L, "NVDA", "NVIDIA Corp",
                new BigDecimal("100"), new BigDecimal("140.00"),
                new BigDecimal("125.60"), new BigDecimal("121.40"),
                new BigDecimal("12560.00"), new BigDecimal("14000.00"),
                new BigDecimal("-1440.00"), -10.29,
                new BigDecimal("420.00"), 3.46, 0.556738, true);
    }

    private static PagedResponse<HoldingRow> pageOfOne() {
        return PagedResponse.from(
                new PageImpl<>(List.of(holdingRow()), PageRequest.of(0, 20), 1),
                new Freshness(AS_OF, false));
    }
}
