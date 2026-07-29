package com.fnpis.repository;

import com.fnpis.domain.ArticleSecurityLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleSecurityLinkRepository
        extends JpaRepository<ArticleSecurityLink, ArticleSecurityLink.Key> {

    List<ArticleSecurityLink> findByArticleId(Long articleId);
}
