package com.fnpis.repository;

import com.fnpis.domain.ArticleSecurityLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleSecurityLinkRepository
        extends JpaRepository<ArticleSecurityLink, ArticleSecurityLink.Key> {
}
