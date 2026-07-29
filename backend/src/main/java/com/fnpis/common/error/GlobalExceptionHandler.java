package com.fnpis.common.error;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The only place error responses are produced (API contract 6.1).
 *
 * <p>Every response shares one skeleton - {@code type}, {@code title},
 * {@code status}, {@code detail}, {@code instance}, {@code code}. Validation
 * failures <b>append</b> an {@code errors} array; they never reshape the
 * skeleton, so a client needs a single parse path.
 *
 * <p><b>Extending {@link ResponseEntityExceptionHandler} is load-bearing.</b>
 * Spring raises around a dozen exceptions before a controller ever runs - wrong
 * path-variable type, unsupported verb, non-JSON {@code Content-Type}, no such
 * route - and already knows the right 4xx for each. A bare {@code @ControllerAdvice}
 * with an {@code Exception} catch-all intercepts every one of them and answers
 * 500, because {@code ExceptionHandlerExceptionResolver} runs ahead of
 * {@code DefaultHandlerExceptionResolver}. Inheriting keeps that mapping table;
 * {@link #handleExceptionInternal} then adds the {@code code} field Spring's
 * bare {@code ProblemDetail} lacks.
 *
 * <p>Do not add {@code @ExceptionHandler} methods for exceptions the parent
 * already maps - two methods claiming one type fails context startup with
 * {@code Ambiguous @ExceptionHandler}. Override the parent hook instead.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://api.fnpis.local/errors/";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.code().status(), ex.getMessage());
        decorate(body, ex.code(), request);
        logByStatus(ex, ex.code(), request);
        return ResponseEntity.status(ex.code().status()).body(body);
    }

    /**
     * Bean-validation failures on a request body (contract 6.1).
     *
     * <p>An override rather than a new handler: the parent maps this type, so a
     * second {@code @ExceptionHandler} for it would be ambiguous.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue())))
                .toList();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                status, "请求有 " + errors.size() + " 个字段不合法");
        // Set before handleExceptionInternal so decorate() sees it already present
        // and leaves it alone - the errors array is an addition to the skeleton,
        // never a replacement for part of it.
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Unparseable request body - malformed JSON, a string where a number belongs,
     * bytes that are not valid UTF-8.
     *
     * <p>Overridden only to replace Spring's terse detail with one a frontend
     * developer can act on. The 400 itself comes from the parent.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "请求体不是合法的 JSON");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * The single exit every inherited handler passes through.
     *
     * <p>Completing the skeleton here rather than in each override is what keeps
     * it constant: a handler we never wrote - some future Spring version adding
     * one - still produces a response carrying {@code code}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ErrorCode code = ErrorCode.forStatus(status);
        ProblemDetail problem = body instanceof ProblemDetail existing
                ? existing
                : ProblemDetail.forStatusAndDetail(status, fallbackDetail(status));
        decorate(problem, code, request);
        logByStatus(ex, code, request);
        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * Genuine server-side faults.
     *
     * <p>Safe to keep as a catch-all now: everything the framework maps is claimed
     * by a more specific inherited handler, so only unhandled exceptions from our
     * own code land here.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                ErrorCode.INTERNAL_ERROR.status(), "服务器内部错误");
        decorate(body, ErrorCode.INTERNAL_ERROR, request);
        logByStatus(ex, ErrorCode.INTERNAL_ERROR, request);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status()).body(body);
    }

    /**
     * Stamps the four skeleton fields Spring does not set.
     *
     * <p>One implementation for both paths - business {@link ApiException} and
     * framework-raised - because two would drift.
     */
    private void decorate(ProblemDetail body, ErrorCode code, WebRequest request) {
        body.setType(URI.create(TYPE_PREFIX + code.slug()));
        body.setTitle(code.title());
        body.setInstance(URI.create(uriOf(request)));
        body.setProperty("code", code.name());
        if (body.getDetail() == null || body.getDetail().isBlank()) {
            body.setDetail(fallbackDetail(code.status()));
        }
    }

    /**
     * 5xx gets a stack trace, 4xx gets one line.
     *
     * <p>A client sending bad input is not an incident. Logging traces for 4xx
     * buries the 5xx that matter under noise nobody on this side can fix.
     */
    private void logByStatus(Exception ex, ErrorCode code, WebRequest request) {
        if (code.status().is5xxServerError()) {
            LOG.error("Unhandled exception on {}", uriOf(request), ex);
        } else {
            LOG.warn("{} on {}: {}", code.name(), uriOf(request), ex.getMessage());
        }
    }

    private static String uriOf(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        // Non-servlet request (test harness, future reactive stack). Better a
        // description than a null instance field breaking the skeleton.
        return request.getDescription(false);
    }

    private static String fallbackDetail(HttpStatusCode status) {
        return "请求失败（HTTP " + status.value() + "）";
    }
}
