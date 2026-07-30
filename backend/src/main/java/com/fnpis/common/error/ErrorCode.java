package com.fnpis.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Stable error codes (API contract 6).
 *
 * <p>Clients branch on {@code code}, never on {@code detail} - the prose is
 * free to change, these constants are not. Renaming one is a breaking API
 * change; appending a constant is not.
 *
 * <p>The four framework-level codes ({@code ENDPOINT_NOT_FOUND},
 * {@code METHOD_NOT_ALLOWED}, {@code NOT_ACCEPTABLE},
 * {@code UNSUPPORTED_MEDIA_TYPE}) are <b>not yet in contract 6</b>. They cover
 * failures Spring raises before a controller runs, which that table never
 * enumerated. Documented in doc 8 section 5 until the contract catches up.
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

    /**
     * No route matches the path.
     *
     * <p>Distinct from the business 404s above: those come from an
     * {@link ApiException} raised by a service that looked something up and found
     * nothing. This one means the URL itself is wrong, so no lookup ever happened.
     */
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "Endpoint not found"),

    /** Path exists, verb does not - {@code PUT} on a PATCH-only resource. */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),

    /** {@code Accept} asks for a representation we do not produce. */
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "Not acceptable"),

    /** {@code Content-Type} is something other than JSON. */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),

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

    /**
     * The code for a status Spring decided on its own.
     *
     * <p>Framework failures arrive as a status with no business meaning attached -
     * a malformed path variable never reaches a service, so nothing throws
     * {@link ApiException}. This is the reverse lookup that gives those responses
     * the {@code code} field clients branch on (contract 6).
     *
     * <p>Anything not listed maps to {@code INTERNAL_ERROR}: an unrecognised
     * status means we do not know what happened, and saying so is better than
     * inventing a client-facing reason.
     */
    public static ErrorCode forStatus(HttpStatusCode status) {
        if (status == null) {
            return INTERNAL_ERROR;
        }
        return switch (status.value()) {
            case 400 -> VALIDATION_FAILED;
            case 401 -> API_KEY_INVALID;
            case 403 -> ADMIN_TOKEN_INVALID;
            // Route-level, not business-level: the business 404s all travel as
            // ApiException and never come through here.
            case 404 -> ENDPOINT_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 406 -> NOT_ACCEPTABLE;
            case 409 -> TASK_ALREADY_RUNNING;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 503 -> UPSTREAM_UNAVAILABLE;
            default -> INTERNAL_ERROR;
        };
    }
}
