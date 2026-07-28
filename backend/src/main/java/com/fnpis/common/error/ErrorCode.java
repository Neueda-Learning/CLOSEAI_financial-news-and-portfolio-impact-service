package com.fnpis.common.error;

import org.springframework.http.HttpStatus;

/**
 * Stable error codes (API contract 6).
 *
 * <p>Clients branch on {@code code}, never on {@code detail} - the prose is
 * free to change, these constants are not. Renaming one is a breaking API
 * change.
 */
public enum ErrorCode {

    /** Bad request body or query parameter (EC-06, EC-08, EC-10). */
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),

    /** Unknown ticker (EC-05). */
    SECURITY_NOT_FOUND(HttpStatus.NOT_FOUND, "Security not found"),

    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "Portfolio not found"),

    HOLDING_NOT_FOUND(HttpStatus.NOT_FOUND, "Holding not found"),

    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Article not found"),

    /** Admin endpoint called without a valid {@code X-Admin-Token}. */
    ADMIN_TOKEN_INVALID(HttpStatus.FORBIDDEN, "Admin token invalid"),

    /** Public read-only API called without a valid {@code X-API-Key}. */
    API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "API key invalid"),

    /**
     * A refresh or recompute is already in flight. Returned when the execution
     * lock is held, so a manual demo click colliding with the scheduled run
     * gets a clear answer instead of silence (EC-20).
     */
    TASK_ALREADY_RUNNING(HttpStatus.CONFLICT, "Task already running"),

    /**
     * Upstream is down <b>and</b> nothing cached exists to serve (EC-11).
     *
     * <p>This should be rare. Decision 2 means a dead provider yields cached
     * data with {@code stale: true}, not an error. Only a symbol we have never
     * successfully fetched reaches this.
     */
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Upstream unavailable"),

    /** Anything unmapped. Always accompanied by a logged stack trace. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    /** Kebab-case slug used to build the RFC 7807 {@code type} URI. */
    public String slug() {
        return name().toLowerCase().replace('_', '-');
    }
}
