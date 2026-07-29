package com.fnpis.repository;

import com.fnpis.domain.NewsArticle;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Optional<NewsArticle> findByExternalId(String externalId);

    List<NewsArticle> findByPublishedAtBetween(Instant start, Instant end);
}
