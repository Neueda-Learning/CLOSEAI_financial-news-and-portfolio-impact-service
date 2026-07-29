package com.fnpis.api.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import com.fnpis.scheduler.ImpactRecomputeScheduler;
import com.fnpis.service.AdminTokenGuard;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The recompute endpoint at the HTTP boundary.
 *
 * <p>{@link com.fnpis.service.AdminTokenGuardTest} already covers the guard's own
 * decisions. What only a real dispatch can show is that the guard is actually
 * reached, and reached <i>before</i> the work starts - a controller that ran the
 * recompute and then checked the token would pass every unit test in this
 * project while leaving the endpoint effectively open.
 *
 * <p>{@code @WebMvcTest} loads the web layer only, so this needs no database.
 */
@WebMvcTest(ImpactAdminController.class)
class ImpactAdminControllerTest {

    private static final String PATH = "/api/v1/impacts/recompute";
    private static final String IDENTITY = "admin-token:demo";
    private static final String TOKEN = "s3cret-demo-token";

    @Autowired private MockMvc mvc;

    @MockBean private ImpactRecomputeScheduler scheduler;
    @MockBean private AdminTokenGuard guard;

    @Nested
    @DisplayName("authorisation happens before any work")
    class Authorisation {

        @Test
        @DisplayName("a refused token recomputes nothing")
        void refusedTokenDoesNoWork() throws Exception {
            willThrow(new ApiException(ErrorCode.ADMIN_TOKEN_INVALID, "nope"))
                    .given(guard).authorise(any());

            mvc.perform(post(PATH)
                            .header("X-Admin-Token", "wrong")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"portfolioId\":7}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ADMIN_TOKEN_INVALID"));

            // The assertion that matters: the session was never rewritten.
            then(scheduler).should(never()).manualRecompute(any(), any());
        }

        @Test
        @DisplayName("a disabled endpoint answers 404 and still does no work")
        void disabledIsNotFound() throws Exception {
            willThrow(new ApiException(ErrorCode.ENDPOINT_NOT_FOUND, "no handler"))
                    .given(guard).authorise(any());

            mvc.perform(post(PATH)
                            .header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());

            then(scheduler).should(never()).manualRecompute(any(), any());
        }

        @Test
        @DisplayName("a missing header reaches the guard rather than failing to bind")
        void missingHeaderIsNotABindError() throws Exception {
            willThrow(new ApiException(ErrorCode.ADMIN_TOKEN_INVALID, "nope"))
                    .given(guard).authorise(null);

            // required = false on the header matters: a bind failure would answer
            // 400 and bypass the 404/403 distinction the contract asks for.
            mvc.perform(post(PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ADMIN_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("an authorised call")
    class Authorised {

        @Test
        @DisplayName("passes the body's scope through and reports the audit identity")
        void recomputesAndAudits() throws Exception {
            given(guard.authorise(TOKEN)).willReturn(IDENTITY);
            given(scheduler.manualRecompute(eq(7L), eq(LocalDate.of(2026, 7, 27))))
                    .willReturn(14);

            mvc.perform(post(PATH)
                            .header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"portfolioId\":7,\"date\":\"2026-07-27\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recomputed").value(14))
                    .andExpect(jsonPath("$.portfolioId").value(7))
                    .andExpect(jsonPath("$.date").value("2026-07-27"))
                    // The identity, never the token.
                    .andExpect(jsonPath("$.triggeredBy").value(IDENTITY));
        }

        @Test
        @DisplayName("an absent body means every portfolio, today")
        void emptyBodyMeansAll() throws Exception {
            given(guard.authorise(TOKEN)).willReturn(IDENTITY);
            given(scheduler.manualRecompute(eq(null), any(LocalDate.class))).willReturn(31);

            mvc.perform(post(PATH).header("X-Admin-Token", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recomputed").value(31))
                    // Null rather than absent-and-guessed: the caller asked for
                    // everything, and the response says so.
                    .andExpect(jsonPath("$.portfolioId").doesNotExist());
        }

        @Test
        @DisplayName("EC-20: a run already in flight is a 409")
        void alreadyRunningIsConflict() throws Exception {
            given(guard.authorise(TOKEN)).willReturn(IDENTITY);
            willThrow(ApiException.taskAlreadyRunning("impact-recompute"))
                    .given(scheduler).manualRecompute(any(), any());

            mvc.perform(post(PATH)
                            .header("X-Admin-Token", TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TASK_ALREADY_RUNNING"));
        }
    }
}
