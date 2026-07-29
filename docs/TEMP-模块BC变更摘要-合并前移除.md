# 模块 B/C 变更摘要（临时文档，合并前移除）

> 分支：`feature/module-bc-providers`
> 日期：2026-07-29

---

## 新增数据字段

### news_article.image（VARCHAR 1024）

新闻封面缩略图 URL。来源：Finnhub `/company-news` 的 `image` 字段。

**影响文件**：

| 文件 | 改动 |
|------|------|
| `V9__add_image_column.sql` | ALTER TABLE news_article ADD COLUMN image |
| `NewsArticle.java` | 新增 `@Column(name="image") private String image` |
| `NewsItem.java` | record 新增 `String image` |
| `FinnhubNewsProvider.java` | 映射 `r.image()` |
| `NewsPersistenceService.java` | `toEntity` 新增 `a.setImage()` |

### news_article.summary（TEXT）

新闻摘要。来源：Finnhub `/company-news` 的 `summary` 字段。
决策：落库不分析。D/E 确认只用标题（AS-04），但存 summary 以备后用。

**影响文件**：

| 文件 | 改动 |
|------|------|
| `V8__add_summary_column.sql` | ALTER TABLE + match_method COMMENT 更新 |
| `NewsArticle.java` | 新增 `@Column(name="summary", columnDefinition="TEXT")` |
| `MatchMethod.java` | Javadoc 标注 V2 注释漂移 |

---

## 合并前检查

- [ ] 本文档已删除
- [ ] V8 + V9 migration 序号与 dev 上的最新 migration 不冲突
- [ ] NewsArticle 新增列已告知 schema owner（CLAUDE.md Ownership 表）

---

> ⚠️ 本文档为临时变更记录，**必须在合并到 dev 之前移除**。
> 最终变更记录以 commit log 和 PR description 为准。
