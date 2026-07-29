package com.fnpis.service;

import com.fnpis.api.internal.dto.SecurityOption;
import com.fnpis.domain.Security;
import com.fnpis.repository.SecurityRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Security lookup for the add-holding picker (A4, EC-05).
 *
 * <p>Read-only: the watchlist is seeded by {@code V5__seed_watchlist.sql} and
 * does not grow at runtime (AS-03). There is no create, update or delete here
 * and there should not be - adding a symbol is a migration, not an API call.
 *
 * <p>The point of the endpoint is to stop EC-05 from being the normal path.
 * Without a picker the user guesses a code, and every guess outside the fifteen
 * seeded rows comes back 404 - correct behaviour, terrible interaction. The
 * error stays as the backstop for a hand-typed symbol.
 */
@Service
public class SecurityService {

    /**
     * Suggestion cap. The dropdown is scanned by eye, not scrolled, and the whole
     * table is fifteen rows, so this is about the widget rather than the query
     * cost. Kept as a limit anyway: the contract leaves search behaviour open
     * (9), and if the watchlist is ever widened this is the line that stops an
     * unbounded response instead of a rewrite.
     */
    private static final int MAX_SUGGESTIONS = 20;

    private final SecurityRepository securities;

    SecurityService(SecurityRepository securities) {
        this.securities = securities;
    }

    /**
     * Symbols matching {@code query}, or the whole watchlist when it is absent.
     *
     * <p>A blank query returning everything is the deliberate choice: fifteen
     * rows is a browsable list, and a user who has not typed yet needs to see
     * what the system accepts at all. Returning nothing until the third keypress
     * hides the whitelist from the person who most needs it.
     *
     * <p>Wildcards are escaped before they reach the LIKE. An unescaped {@code %}
     * from the client would match every row - not a security hole against a
     * read-only seeded table, but a confusing result that looks like a bug.
     */
    @Transactional(readOnly = true)
    public List<SecurityOption> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            // Sorted explicitly, to match the ORDER BY on the search query. The
            // symbol is the primary key, so InnoDB happens to return these in
            // alphabetical order anyway and the two paths look identical today -
            // but that is the storage engine's choice, not a guarantee. Without
            // this the list could silently start arriving in a different order
            // than the one the same user sees after typing one character.
            return securities.findAll(PageRequest.of(0, MAX_SUGGESTIONS, Sort.by("symbol"))).stream()
                    .map(SecurityService::optionFor)
                    .toList();
        }

        String escaped = escapeWildcards(trimmed);
        return securities.search(
                        escaped + "%",
                        "%" + escaped + "%",
                        PageRequest.of(0, MAX_SUGGESTIONS))
                .stream()
                .map(SecurityService::optionFor)
                .toList();
    }

    /**
     * Neutralises the three characters LIKE treats as syntax.
     *
     * <p>Backslash first, or the escapes added for {@code %} and {@code _} would
     * themselves be escaped on the next pass and the pattern would be wrong.
     */
    private static String escapeWildcards(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static SecurityOption optionFor(Security s) {
        return new SecurityOption(s.getSymbol(), s.getCompanyName());
    }
}
