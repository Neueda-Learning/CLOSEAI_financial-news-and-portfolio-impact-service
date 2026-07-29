package com.fnpis.service;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the {@code X-Admin-Token} header on destructive endpoints
 * (API contract 4.4, "管理端点的安全边界").
 *
 * <p>Only guards operations that change global state. Recompute overwrites a
 * whole session's assessments, so an accidental call during a demo is not a
 * harmless retry - it rewrites the numbers on screen.
 *
 * <p><b>Absent and wrong are answered differently on purpose.</b> When admin is
 * switched off the endpoint is not there at all and the answer is 404, matching
 * the contract's rule that a disabled endpoint must not advertise its own
 * existence. When admin is on and the token is wrong, that is a 403 - the caller
 * found a real endpoint and failed to authenticate.
 *
 * <p>Comparison is constant-time. A token check that returns early on the first
 * differing byte leaks the shared secret's prefix to anyone who can time the
 * response, and {@link String#equals} does exactly that.
 */
@Component
public class AdminTokenGuard {

    private final boolean enabled;
    private final String token;
    private final String identity;

    AdminTokenGuard(
            @Value("${app.admin.enabled}") boolean enabled,
            @Value("${app.admin.token}") String token,
            @Value("${app.admin.identity}") String identity) {
        this.enabled = enabled;
        this.token = token;
        this.identity = identity;
    }

    /**
     * Authorises an admin call.
     *
     * @param presented the {@code X-Admin-Token} header, null when absent
     * @return the configured admin identity, for the response's audit fields
     * @throws ApiException 404 when admin is disabled, 403 when the token is
     *         missing or wrong
     */
    public String authorise(String presented) {
        if (!enabled) {
            // Deliberately the same answer as an unknown path: whether this
            // endpoint exists is itself information.
            throw new ApiException(
                    ErrorCode.ENDPOINT_NOT_FOUND, "No handler for this path");
        }
        // An enabled guard with no token configured is a deployment mistake, not
        // an open door. Treat every call as unauthorised rather than letting a
        // blank header match a blank secret.
        if (token == null || token.isBlank() || !matches(presented)) {
            throw new ApiException(
                    ErrorCode.ADMIN_TOKEN_INVALID, "X-Admin-Token missing or invalid");
        }
        return identity;
    }

    /** Constant-time comparison, so response timing reveals nothing. */
    private boolean matches(String presented) {
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
