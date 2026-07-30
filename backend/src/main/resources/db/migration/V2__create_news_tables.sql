-- News, its link to instruments, and the sentiment verdict (modules C and D).

-- One financial news item (C1). Only the headline is analysed - article bodies
-- are out of scope (AS-04, section 12).
CREATE TABLE news_article (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  -- The whole deduplication mechanism is this one constraint. Without it the
  -- 15-minute poll inserts the same story on every run and SC-005 fails.
  -- Kept at 128 chars on purpose: a utf8mb4 single-column index tops out
  -- around 191 characters, so widening this later would break the index.
  external_id  VARCHAR(128)  NOT NULL COMMENT 'Provider-side unique id (C2, 5.4)',
  headline     VARCHAR(512)  NOT NULL COMMENT 'Truncate rather than switch to TEXT (4.4)',
  source       VARCHAR(64)   NOT NULL,
  url          VARCHAR(1024) NOT NULL,
  published_at DATETIME(3)   NOT NULL COMMENT 'UTC; drives the attribution date (EC-16, EC-17)',
  -- When we pulled it, as opposed to when it was published. The news list
  -- reports asOf from this column, which is what lets the UI mark data stale
  -- while a provider is down (decision 2, B4).
  fetched_at   DATETIME(3)   NOT NULL COMMENT 'UTC',
  PRIMARY KEY (id),
  CONSTRAINT uq_article_external_id UNIQUE (external_id),
  -- The list is always newest-first and paged 20 at a time (C3).
  KEY idx_article_published_at (published_at DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Which instruments a story concerns (5.5). Genuinely many-to-many: one story
-- can move several holdings ("chip stocks rally", EC-24) and one instrument
-- collects many stories. Putting a single symbol column on news_article is the
-- modelling mistake this table exists to prevent (architecture 4.3 point 2).
CREATE TABLE article_security_link (
  article_id   BIGINT      NOT NULL,
  symbol       VARCHAR(16) NOT NULL,
  match_method VARCHAR(24) NOT NULL COMMENT 'SYMBOL_EXACT | NAME_FUZZY - stored as a string (6.2)',
  -- Composite key, so re-running the poll cannot duplicate a link.
  PRIMARY KEY (article_id, symbol),
  CONSTRAINT fk_link_article FOREIGN KEY (article_id)
      REFERENCES news_article (id) ON DELETE CASCADE,
  -- Safe because decision 4 fetches per watchlist symbol, so the symbol we
  -- link is by construction already in security. If fuzzy matching ever lands,
  -- unknown symbols must be upserted into security before linking here.
  CONSTRAINT fk_link_security FOREIGN KEY (symbol)
      REFERENCES security (symbol),
  KEY idx_link_symbol (symbol) COMMENT 'Filter the news list by ticker (C4)'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- The sentiment verdict on a headline (D1, D2). Written once and read forever:
-- an LLM is not bit-reproducible even at temperature 0, so "reproducible" is
-- delivered by persisting the result, not by re-running the model (5.2,
-- decision 5 cost 1).
CREATE TABLE sentiment_score (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  article_id    BIGINT       NOT NULL,
  label         VARCHAR(16)  NOT NULL COMMENT 'POSITIVE | NEGATIVE | NEUTRAL, never null (5.2)',
  score         DECIMAL(5,4) NOT NULL COMMENT '-1..1, sign must agree with label',
  confidence    DECIMAL(5,4) NOT NULL COMMENT '0..1',
  -- Multi-engine comparison was dropped, but this stays: after a model or
  -- prompt change it is the only way to tell which version produced a verdict
  -- (architecture 4.3 point 3).
  model_version VARCHAR(64)  NOT NULL COMMENT 'Model name + prompt version, e.g. agent-v1',
  analyzed_at   DATETIME(3)  NOT NULL COMMENT 'UTC',
  PRIMARY KEY (id),
  -- One verdict per article. This also stops the analysis job from paying for
  -- the same headline twice: the job upserts, so a re-run costs no LLM calls
  -- (decision 5 cost 2).
  CONSTRAINT uq_sentiment_article UNIQUE (article_id),
  CONSTRAINT fk_sentiment_article FOREIGN KEY (article_id)
      REFERENCES news_article (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
