-- Intraday price points: the curve on the right-hand side of the linked view.
--
-- Required by the API contract's impact-view response, which is the single most
-- important endpoint in the project (contract section on impact-view, demo
-- script step 4):
--
--   "priceSeries": { "newsMarker": "2026-07-27T12:31:00Z", "points": [
--       { "t": "2026-07-27T12:00:00Z", "price": "121.85" },
--       { "t": "2026-07-27T12:31:00Z", "price": "121.90" } ] }
--
-- Minute-level timestamps within one session. Neither existing table can serve
-- that: price_quote keeps one row per symbol and overwrites it, price_bar keeps
-- one close per session. Without this table newsMarker has no axis to sit on and
-- the linked chart cannot be drawn at all.
--
-- Missing from architecture 4.2 - the third gap found in that table list, after
-- the impact_assessment field set and portfolio_valuation_snapshot.
--
-- START WRITING ROWS NOW. Free data sources do not sell historical minute bars,
-- so a session that went unrecorded is gone for good. This is worse than the
-- daily-bar gap, which at least has the quote-derived fallback in architecture
-- 6.3. Every day without the quote refresh appending here is a day the demo
-- chart cannot cover.
CREATE TABLE price_point (
  symbol      VARCHAR(16)   NOT NULL,
  -- Provider capture time, never our write time. newsMarker is aligned onto
  -- this axis by the backend, so filling it with now() at write time shifts the
  -- annotation line away from where the price actually moved. Same rule as
  -- price_quote.as_of.
  captured_at DATETIME(3)   NOT NULL COMMENT 'UTC, provider capture time',
  price       DECIMAL(18,4) NOT NULL,
  -- Composite key in query order: impact-view always reads one symbol over one
  -- session, so this covers it without a secondary index. It also makes a
  -- re-run overwrite rather than duplicate.
  PRIMARY KEY (symbol, captured_at),
  -- No ON DELETE CASCADE, unlike price_bar. These rows cannot be re-collected
  -- from anywhere, so a future "remove from watchlist" feature must be forced
  -- to deal with them explicitly instead of silently dropping the history.
  CONSTRAINT fk_point_security FOREIGN KEY (symbol)
      REFERENCES security (symbol)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Retention: nothing prunes this table, deliberately. One point a minute over a
-- 6.5-hour session across 15 symbols is roughly 5.9k rows a day - a month of
-- demo prep is under 200k, which MySQL does not notice. A cleanup job would add
-- a second way to lose data that cannot be re-collected, for no benefit at this
-- scale. Revisit only if the watchlist or the polling frequency grows by an
-- order of magnitude.
