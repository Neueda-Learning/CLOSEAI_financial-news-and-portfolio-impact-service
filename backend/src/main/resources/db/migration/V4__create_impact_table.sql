-- What one news story did to one holding on one session (module E). The
-- project's only original logic, and the reason the linked view has anything
-- to show.
--
-- Computed by the scheduler and stored, never derived inside a read request:
-- the numbers depend on prices at a point in time, so recomputing on read
-- would return a different answer every time and nothing would be
-- reproducible on demo day (decision 3).
--
-- Ratios are stored as ratios, not percentages. price_change_ratio 0.0415 is
-- the +4.15% the contract renders; the API multiplies by 100 on the way out.
-- Storing percentages here would put the x100 in two places and they would
-- eventually disagree.
CREATE TABLE impact_assessment (
  id                    BIGINT        NOT NULL AUTO_INCREMENT,
  article_id            BIGINT        NOT NULL,
  symbol                VARCHAR(16)   NOT NULL,
  portfolio_id          BIGINT        NOT NULL COMMENT 'Value impact depends on this portfolio''s weights',
  attribution_date      DATE          NOT NULL COMMENT 'Session the story is charged to (5.3 step 1)',
  holding_weight        DECIMAL(10,6) NOT NULL COMMENT 'w: holding value / portfolio value',
  price_change_ratio    DECIMAL(10,6)     NULL COMMENT 'r; null when the prior close is missing (EC-18)',
  expected_impact       DECIMAL(10,6) NOT NULL COMMENT 's * c * w, signed',
  observed_contribution DECIMAL(10,6)     NULL COMMENT 'w * r',
  value_impact          DECIMAL(18,4)     NULL COMMENT 'holding value * r, the figure shown on screen',
  direction             VARCHAR(24)   NOT NULL COMMENT 'POSITIVE | NEGATIVE | NEUTRAL, from sentiment (E2)',
  alignment             VARCHAR(24)   NOT NULL COMMENT 'CONFIRMED | DIVERGENT | INCONCLUSIVE (E4)',
  computed_at           DATETIME(3)   NOT NULL COMMENT 'UTC; answers "when was this concluded"',
  PRIMARY KEY (id),
  -- EC-24 gives every affected holding its own row, so the key has to include
  -- the symbol. With it, /impacts/recompute is an idempotent upsert; without
  -- it, recompute has to delete first and a mid-run failure leaves a hole in
  -- the day's data.
  CONSTRAINT uq_impact_article_symbol_portfolio_date
      UNIQUE (article_id, symbol, portfolio_id, attribution_date),
  CONSTRAINT fk_impact_article FOREIGN KEY (article_id)
      REFERENCES news_article (id) ON DELETE CASCADE,
  CONSTRAINT fk_impact_portfolio FOREIGN KEY (portfolio_id)
      REFERENCES portfolio (id) ON DELETE CASCADE,
  -- Deliberately no FK to holding: a row records what a story did to a symbol
  -- in a portfolio, not to one holding row. Deleting a position (A6) must not
  -- erase the history that was already assessed against it.
  CONSTRAINT fk_impact_security FOREIGN KEY (symbol)
      REFERENCES security (symbol),
  KEY idx_impact_portfolio_date (portfolio_id, attribution_date)
      COMMENT 'Impact list and the daily summary (E1-E3, E5)'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
