package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Which instruments a story concerns (5.5).
 *
 * <p>Genuinely many-to-many: one story can move several holdings ("chip stocks
 * rally", EC-24) and one instrument collects many stories. A single symbol
 * column on {@link NewsArticle} is the modelling mistake this table exists to
 * prevent (architecture 4.3 point 2).
 *
 * <p>The composite key is the whole row, so re-running the poll cannot
 * duplicate a link.
 */
@Entity
@Table(name = "article_security_link")
@IdClass(ArticleSecurityLink.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class ArticleSecurityLink {

    @Id
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Id
    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /** Stored as a string, never an ordinal (architecture 6.2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", nullable = false, length = 24)
    private MatchMethod matchMethod;

    public ArticleSecurityLink(Long articleId, String symbol, MatchMethod matchMethod) {
        this.articleId = articleId;
        this.symbol = symbol;
        this.matchMethod = matchMethod;
    }

    /** Composite primary key. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long articleId;
        private String symbol;

        public Key(Long articleId, String symbol) {
            this.articleId = articleId;
            this.symbol = symbol;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(articleId, key.articleId) && Objects.equals(symbol, key.symbol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(articleId, symbol);
        }
    }
}
