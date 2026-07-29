-- Persist the provider summary for future use (sentiment analyses the headline
-- only per AS-04). Once the free-tier history window slides past an article, the
-- summary cannot be re-fetched — same reasoning as price_point rows.

ALTER TABLE news_article
    ADD COLUMN summary TEXT NULL
    AFTER headline;

ALTER TABLE article_security_link
    MODIFY COLUMN match_method VARCHAR(24) NOT NULL
    COMMENT 'SYMBOL_EXACT | NAME_FUZZY | SEMANTIC_VECTOR (stored as a string, 6.2)';
