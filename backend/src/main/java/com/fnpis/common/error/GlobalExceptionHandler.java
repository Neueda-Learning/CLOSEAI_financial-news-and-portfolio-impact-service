package com.fnpis.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The only place error responses are produced (API contract 6.1).
 *
 * <p>Every response shares one skeleton - {@code type}, {@code title},
 * {@code status}, {@code detail}, {@code instance}, {@code code}. Validation
 * failures <b>append</b> an {@code errors} array; they never reshape the
 * skeleton, so a client needs a single parse path.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://api.fnpis.local/errors/";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(ex.code(), ex.getMessage(), request, null);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ProblemDetail> handleValidation(BindException ex, HttpServletRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue())))
                .toList();
        String detail = "Request has " + errors.size() + " invalid field(s)";
        return respond(ErrorCode.VALIDATION_FAILED, detail, request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log the trace, return nothing internal to the caller
        LOG.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ProblemDetail> respond(
            ErrorCode code, String detail, HttpServletRequest request, List<FieldError> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.status(), detail);
        body.setType(URI.create(TYPE_PREFIX + code.slug()));
        body.setTitle(code.title());
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("code", code.name());
        if (errors != null && !errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(code.status()).body(body);
    }
}
