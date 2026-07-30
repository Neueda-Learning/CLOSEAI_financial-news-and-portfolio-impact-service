package com.fnpis.api.internal;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.common.error.ApiException;
import com.fnpis.scheduler.SentimentAnalysisScheduler;
import com.fnpis.service.SentimentAnalysisService.AnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The manual sentiment trigger at the HTTP boundary.
 *
 * <p>The scheduler's own tests cover what a run does. What only a real dispatch
 * shows is that the path is reachable at all - the reason this endpoint was
 * written is that {@code manualAnalysis()} existed for a while with no caller,
 * so the capability was present and unusable.
 *
 * <p>The counts are asserted individually rather than as a shape: {@code stored}
 * and {@code rejected} are adjacent ints of the same type, and a mapper that
 * swapped them would report a clean run while the validator was refusing
 * everything.
 */
@WebMvcTest(SentimentRefreshController.class)
class SentimentRefreshControllerTest {

    private static final String PATH = "/api/v1/sentiment/refresh";

    @Autowired private MockMvc mvc;

    @MockBean private SentimentAnalysisScheduler scheduler;

    @Test
    @DisplayName("a completed run reports each count in its own field")
    void reportsCounts() throws Exception {
        given(scheduler.manualAnalysis()).willReturn(new AnalysisResult(true, 12, 9, 2, 1));

        mvc.perform(post(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggered").value(true))
                .andExpect(jsonPath("$.analysed").value(12))
                .andExpect(jsonPath("$.stored").value(9))
                // Distinct from a failure: the validator refused the model's output.
                .andExpect(jsonPath("$.rejected").value(2))
                .andExpect(jsonPath("$.failures").value(1));
    }

    @Test
    @DisplayName("an empty backlog is a 200 with zeros, not an error")
    void emptyBacklogIsNotAnError() throws Exception {
        given(scheduler.manualAnalysis()).willReturn(new AnalysisResult(true, 0, 0, 0, 0));

        // Nothing left to score is the steady state, so it must not read as a
        // fault - during a demo this is the expected second call.
        mvc.perform(post(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggered").value(true))
                .andExpect(jsonPath("$.analysed").value(0));
    }

    @Test
    @DisplayName("EC-20: a run already in flight is a 409, not a queued second run")
    void alreadyRunningIsConflict() throws Exception {
        willThrow(ApiException.taskAlreadyRunning("sentiment-analysis"))
                .given(scheduler).manualAnalysis();

        mvc.perform(post(PATH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TASK_ALREADY_RUNNING"));
    }
}
