package com.fnpis.domain;

/**
 * How a news article got linked to a symbol (requirements 5.5).
 *
 * <p>MVP only ever produces {@link #SYMBOL_EXACT}: news is fetched per ticker
 * from the company-news endpoint, so the association is exact by construction
 * and needs no text matching (decision 4). The enum exists so that adding
 * fuzzy matching later does not require a migration to tell old rows from new
 * ones.
 *
 * <p>Persist with {@code @Enumerated(EnumType.STRING)}.
 */
public enum MatchMethod {

    /** Article came from that symbol's company-news endpoint. 100% accurate. */
    SYMBOL_EXACT,

    /** Reserved: company name or alias found in the headline. Not implemented. */
    NAME_FUZZY
}
