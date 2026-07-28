package com.fnpis.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PagedResponseTest {

    @Test
    @DisplayName("Spring Data page 0 is reported to clients as page 1")
    void convertsToOneBased() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 137);

        var response = PagedResponse.from(page, Freshness.fresh(null));

        // The whole point of this class: clients count from 1, Spring from 0.
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(137);
        assertThat(response.totalPages()).isEqualTo(7);
    }

    @Test
    @DisplayName("size above the cap is clamped to 100")
    void clampsOversizedRequest() {
        assertThat(PagedResponse.clampSize(5000)).isEqualTo(PagedResponse.MAX_SIZE);
    }

    @Test
    @DisplayName("missing or nonsense size falls back to the default")
    void defaultsWhenAbsentOrInvalid() {
        assertThat(PagedResponse.clampSize(null)).isEqualTo(PagedResponse.DEFAULT_SIZE);
        assertThat(PagedResponse.clampSize(0)).isEqualTo(PagedResponse.DEFAULT_SIZE);
        assertThat(PagedResponse.clampSize(-1)).isEqualTo(PagedResponse.DEFAULT_SIZE);
    }

    @Test
    @DisplayName("a valid size is passed through unchanged")
    void keepsValidSize() {
        assertThat(PagedResponse.clampSize(50)).isEqualTo(50);
    }
}
