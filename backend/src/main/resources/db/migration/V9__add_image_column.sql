-- Thumbnail URL from the provider, for the news list in the frontend.
--
-- TEXT rather than the VARCHAR(1024) used by news_article.url: these are CDN
-- URLs carrying signing and resize parameters, which run past 1024 characters.
-- The deliberate VARCHAR-over-TEXT choice on headline (V2, 4.4) was about
-- truncating prose to a known display limit; a truncated URL is simply broken,
-- so there is nothing to gain by capping it.
--
-- Nullable: not every article ships an image.

ALTER TABLE news_article
    ADD COLUMN image TEXT NULL
    AFTER url;
