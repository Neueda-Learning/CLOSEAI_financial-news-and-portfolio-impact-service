package com.fnpis.api.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.service.HoldingService;
import com.fnpis.service.PortfolioService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * The error contract (API contract 6 and 6.1), enforced at the HTTP boundary.
 *
 * <p>Service-layer tests cannot cover this: status codes and the problem+json
 * skeleton are produced by {@code GlobalExceptionHandler} interacting with
 * Spring's own exception mapping, and only a real dispatch exercises that. The
 * regression this file exists to prevent is a catch-all handler swallowing
 * framework exceptions and answering 500 where the contract promises a 4xx.
 *
 * <p>Assertions read {@code code}, never {@code detail} - contract 6 states the
 * prose is free to change and clients must branch on the code. A test that
 * asserted the wording would be a test of the wording.
 *
 * <p>{@code @WebMvcTest} loads the web layer only: no database, no Testcontainers.
 */
@WebMvcTest(controllers = {PortfolioController.class, HoldingController.class})
@DisplayName("Error contract (contract 6)")
class ErrorContractTest {

    private static final String PORTFOLIOS = "/api/v1/portfolios";
    private static final String VALID_HOLDING = """
            {"symbol":"NVDA","quantity":10,"costBasis":"100.00"}""";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private HoldingService holdingService;

    @Nested
    @DisplayName("Framework-raised failures keep their own status, not 500")
    class FrameworkFailures {

        @Test
        @DisplayName("Non-numeric path variable is 400, not 500")
        void pathVariableTypeMismatch() throws Exception {
            mvc.perform(get(PORTFOLIOS + "/abc/summary"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Non-numeric query parameter is 400, not 500")
        void queryParameterTypeMismatch() throws Exception {
            mvc.perform(get(PORTFOLIOS + "/1/holdings").param("page", "first"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Unsupported verb is 405")
        void methodNotAllowed() throws Exception {
            mvc.perform(put("/api/v1/holdings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_HOLDING))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Non-JSON Content-Type is 415")
        void unsupportedMediaType() throws Exception {
            mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("name=Test"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }

        @Test
        @DisplayName("Unproducible Accept is 406")
        void notAcceptable() throws Exception {
            mvc.perform(get(PORTFOLIOS).accept(MediaType.APPLICATION_XML))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
        }

        @Test
        @DisplayName("Malformed JSON body is 400")
        void malformedJson() throws Exception {
            mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Wrong JSON type for a BigDecimal field is 400")
        void wrongJsonType() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"NVDA\",\"quantity\":\"ten\",\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("Missing body is 400")
        void missingBody() throws Exception {
            mvc.perform(post(PORTFOLIOS).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    @DisplayName("Bean validation (EC-06, EC-08, EC-10, AS-02)")
    class BeanValidation {

        @Test
        @DisplayName("EC-08: fractional shares rejected, field named in errors")
        void fractionalShares() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"NVDA\",\"quantity\":0.5,\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                    .andExpect(jsonPath("$.errors[0].rejectedValue").value("0.5"));
        }

        @Test
        @DisplayName("EC-06: zero quantity rejected")
        void zeroQuantity() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"NVDA\",\"quantity\":0,\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        }

        @Test
        @DisplayName("EC-06: negative quantity rejected")
        void negativeQuantity() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"NVDA\",\"quantity\":-5,\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        }

        @Test
        @DisplayName("Negative cost basis rejected")
        void negativeCostBasis() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"NVDA\",\"quantity\":10,\"costBasis\":\"-1.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("costBasis"));
        }

        @Test
        @DisplayName("EC-10: portfolio name over 100 characters rejected")
        void nameTooLong() throws Exception {
            mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + "x".repeat(101) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));
        }

        @Test
        @DisplayName("EC-10: blank portfolio name rejected")
        void blankName() throws Exception {
            mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"   \"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("name"));
        }

        @Test
        @DisplayName("AS-02: currency other than USD rejected")
        void nonUsdCurrency() throws Exception {
            mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\",\"baseCurrency\":\"EUR\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("baseCurrency"));
        }

        @Test
        @DisplayName("Two bad fields produce two errors entries")
        void twoInvalidFields() throws Exception {
            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"\",\"quantity\":-5,\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(2));
        }
    }

    @Nested
    @DisplayName("Business failures carry their ApiException code")
    class BusinessFailures {

        @Test
        @DisplayName("Missing portfolio is 404 PORTFOLIO_NOT_FOUND")
        void portfolioNotFound() throws Exception {
            given(portfolioService.summary(eq(99L), any()))
                    .willThrow(ApiException.portfolioNotFound(99L));

            mvc.perform(get(PORTFOLIOS + "/99/summary"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
        }

        @Test
        @DisplayName("EC-05: unknown symbol is 404 SECURITY_NOT_FOUND")
        void securityNotFound() throws Exception {
            given(holdingService.add(eq(1L), any(), any()))
                    .willThrow(ApiException.securityNotFound("XYZQ"));

            mvc.perform(post(PORTFOLIOS + "/1/holdings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"symbol\":\"XYZQ\",\"quantity\":10,\"costBasis\":\"100.00\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SECURITY_NOT_FOUND"));
        }

        @Test
        @DisplayName("Missing holding is 404 HOLDING_NOT_FOUND")
        void holdingNotFound() throws Exception {
            willThrow(ApiException.holdingNotFound(42L)).given(holdingService).delete(42L);

            mvc.perform(delete("/api/v1/holdings/42"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("HOLDING_NOT_FOUND"));
        }

        @Test
        @DisplayName("EC-20: contended task is 409 TASK_ALREADY_RUNNING")
        void taskAlreadyRunning() throws Exception {
            given(portfolioService.summary(eq(1L), any()))
                    .willThrow(ApiException.taskAlreadyRunning("quote-refresh"));

            mvc.perform(get(PORTFOLIOS + "/1/summary"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TASK_ALREADY_RUNNING"));
        }

        @Test
        @DisplayName("Empty PATCH body is 400 VALIDATION_FAILED")
        void emptyPatchBody() throws Exception {
            given(holdingService.update(eq(1L), any(), any()))
                    .willThrow(new ApiException(ErrorCode.VALIDATION_FAILED, "至少要修改一个字段"));

            mvc.perform(patch("/api/v1/holdings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("An unmapped exception is 500 INTERNAL_ERROR and leaks nothing")
        void unmappedException() throws Exception {
            given(portfolioService.list(any()))
                    .willThrow(new IllegalStateException("connection pool exhausted at 10.0.0.7:3306"));

            mvc.perform(get(PORTFOLIOS))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    // The cause is logged, never returned - an internal address in a
                    // response body is a disclosure bug.
                    .andExpect(content().string(Matchers.not(Matchers.containsString("10.0.0.7"))));
        }
    }

    @Nested
    @DisplayName("Skeleton stays constant across every path (contract 6.1)")
    class SkeletonShape {

        @Test
        @DisplayName("Framework 400 carries all six skeleton fields")
        void frameworkFailureSkeleton() throws Exception {
            assertSkeleton(mvc.perform(get(PORTFOLIOS + "/abc/summary")), 400);
        }

        @Test
        @DisplayName("Business 404 carries all six skeleton fields")
        void businessFailureSkeleton() throws Exception {
            given(portfolioService.summary(eq(99L), any()))
                    .willThrow(ApiException.portfolioNotFound(99L));

            assertSkeleton(mvc.perform(get(PORTFOLIOS + "/99/summary")), 404);
        }

        @Test
        @DisplayName("Validation 400 adds errors without dropping a skeleton field")
        void validationFailureSkeleton() throws Exception {
            assertSkeleton(
                    mvc.perform(post(PORTFOLIOS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}")),
                    400)
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("500 carries all six skeleton fields")
        void serverFailureSkeleton() throws Exception {
            given(portfolioService.list(any())).willThrow(new IllegalStateException("boom"));

            assertSkeleton(mvc.perform(get(PORTFOLIOS)), 500);
        }

        /** The six fields contract 6.1 fixes, plus the media type. */
        private ResultActions assertSkeleton(ResultActions response, int expectedStatus) throws Exception {
            return response
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(expectedStatus))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").exists())
                    .andExpect(jsonPath("$.code").exists());
        }
    }
}
