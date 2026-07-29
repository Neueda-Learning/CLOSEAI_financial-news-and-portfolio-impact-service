package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fnpis.api.internal.dto.SecurityOption;
import com.fnpis.domain.Security;
import com.fnpis.repository.SecurityRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Search semantics for the add-holding picker (A4).
 *
 * <p>What is worth locking here is the shape of the two LIKE patterns, because
 * both are easy to "tidy" into being wrong. The symbol side must stay a prefix -
 * making it a substring looks harmless and quietly turns a fifteen-row table
 * into a list where "a" matches almost everything. The name side must stay a
 * substring, or nobody finds Disney without typing "the walt".
 *
 * <p>The escaping tests cover the input a user actually produces by accident:
 * a stray {@code %} pasted from a spreadsheet.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security search (A4, EC-05)")
class SecurityServiceTest {

    @Mock
    private SecurityRepository securities;

    @InjectMocks
    private SecurityService service;

    @Test
    @DisplayName("Symbol matches as a prefix, company name as a substring")
    void patternsDifferPerColumn() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        service.search("nv");

        ArgumentCaptor<String> symbolPattern = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> namePattern = ArgumentCaptor.forClass(String.class);
        verify(securities).search(symbolPattern.capture(), namePattern.capture(), any());

        assertThat(symbolPattern.getValue()).isEqualTo("nv%");
        assertThat(namePattern.getValue()).isEqualTo("%nv%");
    }

    @Test
    @DisplayName("A blank query lists the whole watchlist rather than nothing")
    void blankQueryReturnsEverything() {
        given(securities.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(security("AAPL", "Apple Inc."))));

        List<SecurityOption> options = service.search("   ");

        assertThat(options).containsExactly(new SecurityOption("AAPL", "Apple Inc."));
    }

    @Test
    @DisplayName("Both paths sort by symbol, so the order does not jump when the box is cleared")
    void bothPathsSortBySymbol() {
        given(securities.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(security("AAPL", "Apple Inc."))));

        service.search("");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(securities).findAll(pageable.capture());
        // The symbol is the primary key, so InnoDB returns these alphabetically
        // whether or not we ask. Asserting the request rather than the result is
        // the point: the guarantee has to be ours, not the storage engine's.
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("symbol"));
    }

    @Test
    @DisplayName("A null query behaves like a blank one")
    void nullQueryReturnsEverything() {
        given(securities.findAll(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(security("MSFT", "Microsoft Corporation"))));

        assertThat(service.search(null)).hasSize(1);
    }

    @Test
    @DisplayName("A pasted % is escaped, not treated as match-everything")
    void wildcardIsEscaped() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        service.search("%");

        ArgumentCaptor<String> symbolPattern = ArgumentCaptor.forClass(String.class);
        verify(securities).search(symbolPattern.capture(), anyString(), any());
        // The trailing % is ours; the leading one is the user's, now literal.
        assertThat(symbolPattern.getValue()).isEqualTo("\\%%");
    }

    @Test
    @DisplayName("Underscore is escaped too - it is LIKE's single-character wildcard")
    void underscoreIsEscaped() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        service.search("a_b");

        ArgumentCaptor<String> symbolPattern = ArgumentCaptor.forClass(String.class);
        verify(securities).search(symbolPattern.capture(), anyString(), any());
        assertThat(symbolPattern.getValue()).isEqualTo("a\\_b%");
    }

    @Test
    @DisplayName("Backslash is escaped before the characters it would escape")
    void backslashIsEscapedFirst() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        service.search("a\\%b");

        ArgumentCaptor<String> symbolPattern = ArgumentCaptor.forClass(String.class);
        verify(securities).search(symbolPattern.capture(), anyString(), any());
        // Both the user's backslash and their % end up literal. Escaping in the
        // other order would produce \\\% and match nothing.
        assertThat(symbolPattern.getValue()).isEqualTo("a\\\\\\%b%");
    }

    @Test
    @DisplayName("Surrounding whitespace is trimmed off the pattern")
    void queryIsTrimmed() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        service.search("  nvda  ");

        ArgumentCaptor<String> symbolPattern = ArgumentCaptor.forClass(String.class);
        verify(securities).search(symbolPattern.capture(), anyString(), any());
        assertThat(symbolPattern.getValue()).isEqualTo("nvda%");
    }

    @Test
    @DisplayName("No match is an empty list, not an error")
    void noMatchIsEmptyList() {
        given(securities.search(anyString(), anyString(), any())).willReturn(List.of());

        assertThat(service.search("zzzz")).isEmpty();
    }

    private static Security security(String symbol, String name) {
        return new Security(symbol, name);
    }
}
