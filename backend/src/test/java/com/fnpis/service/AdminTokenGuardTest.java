package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fnpis.common.error.ApiException;
import com.fnpis.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The admin guard's two refusals, which must not be confused with each other.
 *
 * <p>Off answers 404 and wrong answers 403. That distinction is the whole point
 * of the contract's rule: a 403 from a disabled endpoint would confirm the
 * endpoint exists to anyone probing for it.
 */
class AdminTokenGuardTest {

    private static final String TOKEN = "s3cret-demo-token";
    private static final String IDENTITY = "admin-token:demo";

    private static AdminTokenGuard enabled() {
        return new AdminTokenGuard(true, TOKEN, IDENTITY);
    }

    private static ErrorCode codeOf(ThrowingCall call) {
        try {
            call.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("expected an ApiException");
    }

    private interface ThrowingCall {
        void run();
    }

    @Nested
    @DisplayName("when admin is switched off")
    class Disabled {

        @Test
        @DisplayName("a correct token still gets 404, not 403")
        void hidesExistence() {
            AdminTokenGuard guard = new AdminTokenGuard(false, TOKEN, IDENTITY);

            // Answering 403 here would tell a prober the endpoint is real and
            // merely locked. 404 is indistinguishable from a wrong URL.
            assertThat(codeOf(() -> guard.authorise(TOKEN)))
                    .isEqualTo(ErrorCode.ENDPOINT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("when admin is on")
    class Enabled {

        @Test
        @DisplayName("the matching token authorises and returns the audit identity")
        void authorises() {
            assertThat(enabled().authorise(TOKEN)).isEqualTo(IDENTITY);
        }

        @Test
        @DisplayName("a missing header is 403")
        void missingHeader() {
            assertThat(codeOf(() -> enabled().authorise(null)))
                    .isEqualTo(ErrorCode.ADMIN_TOKEN_INVALID);
        }

        @Test
        @DisplayName("a wrong token is 403")
        void wrongToken() {
            assertThat(codeOf(() -> enabled().authorise("not-the-token")))
                    .isEqualTo(ErrorCode.ADMIN_TOKEN_INVALID);
        }

        @Test
        @DisplayName("a token that is a prefix of the real one is rejected")
        void prefixRejected() {
            // Guards against a comparison that stops at the shorter length.
            assertThat(codeOf(() -> enabled().authorise("s3cret")))
                    .isEqualTo(ErrorCode.ADMIN_TOKEN_INVALID);
        }

        @Test
        @DisplayName("enabled with no token configured refuses everything")
        void blankSecretIsNotAnOpenDoor() {
            AdminTokenGuard misconfigured = new AdminTokenGuard(true, "", IDENTITY);

            // The dangerous case: a blank header matching a blank secret would
            // turn a deployment mistake into an unauthenticated endpoint.
            assertThatThrownBy(() -> misconfigured.authorise(""))
                    .isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> misconfigured.authorise(null))
                    .isInstanceOf(ApiException.class);
        }
    }
}
