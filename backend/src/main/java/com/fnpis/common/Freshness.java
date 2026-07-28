package com.fnpis.common;

import java.time.Duration;
import java.time.Instant;

/**
 * Data freshness carried on every response that exposes external data.
 *
 * <p>Reads never call a provider inline, so what a client gets is always a
 * snapshot of some age. Rather than hide that, every response states when the
 * data was captured and whether it is past its acceptable age. On demo day
 * this turns a rate limit from a defect into an honest product behaviour, and
 * it is the acceptance point for SC-009 (architecture decision 2).
 *
 * <p>The frontend is required to display both fields (architecture 6.4).
 *
 * @param asOf  when the underlying data was captured, UTC
 * @param stale true when the data is older than its freshness budget, or the
 *              upstream provider is currently unavailable
 */
public record Freshness(Instant asOf, boolean stale) {

    /** Data captured now from a healthy provider. */
    public static Freshness fresh(Instant asOf) {
        return new Freshness(asOf, false);
    }

    /** Data we are serving despite knowing it is behind. */
    public static Freshness stale(Instant asOf) {
        return new Freshness(asOf, true);
    }

    /**
     * Marks the snapshot stale once it is older than {@code budget}.
     *
     * @param asOf   capture time of the data, may be null when nothing has been
     *               fetched yet - treated as stale
     * @param budget how old the data is allowed to get before it counts as stale
     * @param now    current time, injected so this stays testable
     */
    public static Freshness of(Instant asOf, Duration budget, Instant now) {
        if (asOf == null) {
            return new Freshness(null, true);
        }
        return new Freshness(asOf, asOf.plus(budget).isBefore(now));
    }
}
