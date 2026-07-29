package com.fnpis.api.internal.dto;

/**
 * Response body for {@code POST /api/v1/sentiment/refresh} (D3).
 *
 * <p>{@code stored} and {@code rejected} are reported separately rather than as
 * one success count: a rejected verdict is the validator doing its job on
 * malformed model output, not a failure of the run, and collapsing the two would
 * hide the number worth watching during a demo.
 *
 * @param triggered true if the run was accepted (false on 409 conflict)
 * @param analysed headlines sent to the engine this run
 * @param stored verdicts that passed validation and were persisted
 * @param rejected verdicts the validator refused (illegal label, score out of
 *        range, score disagreeing with label)
 * @param failures articles whose analysis threw - a dead engine or a failed write
 */
public record SentimentRefreshResponse(
        boolean triggered,
        int analysed,
        int stored,
        int rejected,
        int failures) {
}
