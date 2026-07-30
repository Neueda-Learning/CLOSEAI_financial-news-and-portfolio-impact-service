package com.fnpis.common;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envelope for every list endpoint (API contract 1.4).
 *
 * <p><b>Page numbers here are 1-based, Spring Data's are 0-based.</b> Convert
 * at the boundary and nowhere else - build the {@code Pageable} with
 * {@code page - 1} and hand the resulting {@link Page} to {@link #from}. Doing
 * the arithmetic anywhere deeper is how off-by-one page bugs get in.
 *
 * @param asOf  see {@link Freshness}
 * @param stale see {@link Freshness}
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Instant asOf,
        boolean stale) {

    /** Highest {@code size} a client may request (API contract 1.4). */
    public static final int MAX_SIZE = 100;

    /** Default page size when the client does not specify one. */
    public static final int DEFAULT_SIZE = 20;

    /**
     * Wraps a Spring Data page, converting back to 1-based numbering.
     *
     * @param freshness data freshness, or null for endpoints serving purely
     *                  internal data where the notion does not apply
     */
    public static <T> PagedResponse<T> from(Page<T> page, Freshness freshness) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                freshness == null ? null : freshness.asOf(),
                freshness != null && freshness.stale());
    }

    /**
     * Clamps a client-supplied size into the allowed range.
     *
     * @param requested raw value off the query string, may be null
     */
    public static int clampSize(Integer requested) {
        if (requested == null || requested < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(requested, MAX_SIZE);
    }
}
