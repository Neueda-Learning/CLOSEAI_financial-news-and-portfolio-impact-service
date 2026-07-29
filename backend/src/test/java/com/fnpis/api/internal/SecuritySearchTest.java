package com.fnpis.api.internal;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fnpis.api.internal.dto.SecurityOption;
import com.fnpis.config.JacksonConfig;
import com.fnpis.service.SecurityService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Endpoint shape for the security picker (contract 2, "securities and quotes").
 *
 * <p>Locks the response as a bare JSON array. The contract leaves search
 * behaviour open (9) but not the envelope, and wrapping this in
 * {@code PagedResponse} later would break the picker without changing any field
 * name - the kind of break that only shows up in the browser.
 */
@WebMvcTest(controllers = SecurityController.class)
@Import(JacksonConfig.class)
@DisplayName("GET /api/v1/securities (A4, EC-05)")
class SecuritySearchTest {

    private static final String SECURITIES = "/api/v1/securities";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SecurityService service;

    @Test
    @DisplayName("Returns a bare array of symbol and name, not a paged envelope")
    void returnsBareArray() throws Exception {
        given(service.search("nv")).willReturn(List.of(
                new SecurityOption("NVDA", "NVIDIA Corporation")));

        mvc.perform(get(SECURITIES).param("q", "nv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].symbol").value("NVDA"))
                .andExpect(jsonPath("$[0].companyName").value("NVIDIA Corporation"))
                // Purely local data, so no freshness pair (contract 1.3).
                .andExpect(jsonPath("$[0].asOf").doesNotExist());
    }

    @Test
    @DisplayName("q is optional - a missing parameter is not a 400")
    void missingQueryIsAccepted() throws Exception {
        given(service.search(isNull())).willReturn(List.of(
                new SecurityOption("AAPL", "Apple Inc."),
                new SecurityOption("MSFT", "Microsoft Corporation")));

        mvc.perform(get(SECURITIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("No match is 200 with an empty array, not 404")
    void noMatchIsEmptyArray() throws Exception {
        given(service.search("zzzz")).willReturn(List.of());

        mvc.perform(get(SECURITIES).param("q", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("The raw query reaches the service; normalising is its job")
    void queryIsPassedThroughUntouched() throws Exception {
        given(service.search("  NvDa  ")).willReturn(List.of());

        mvc.perform(get(SECURITIES).param("q", "  NvDa  "))
                .andExpect(status().isOk());

        verify(service).search("  NvDa  ");
    }
}
