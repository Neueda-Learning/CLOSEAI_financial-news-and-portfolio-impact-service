package com.fnpis.common.error;

/**
 * Business failure that maps onto a known {@link ErrorCode}.
 *
 * <p>Throw this from a service; {@code GlobalExceptionHandler} turns it into
 * the response body. Never build an error body inside a controller - a
 * hand-written one always drifts from the shared skeleton (API contract 6.1).
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ApiException(ErrorCode code, String detail, Throwable cause) {
        super(detail, cause);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static ApiException portfolioNotFound(Long id) {
        return new ApiException(ErrorCode.PORTFOLIO_NOT_FOUND, "Portfolio " + id + " does not exist");
    }

    public static ApiException holdingNotFound(Long id) {
        return new ApiException(ErrorCode.HOLDING_NOT_FOUND, "Holding " + id + " does not exist");
    }

    public static ApiException articleNotFound(Long id) {
        return new ApiException(ErrorCode.ARTICLE_NOT_FOUND, "Article " + id + " does not exist");
    }

    public static ApiException securityNotFound(String symbol) {
        return new ApiException(ErrorCode.SECURITY_NOT_FOUND, "Symbol '" + symbol + "' does not exist");
    }

    public static ApiException taskAlreadyRunning(String taskName) {
        return new ApiException(ErrorCode.TASK_ALREADY_RUNNING, "Task '" + taskName + "' is already running");
    }
}
