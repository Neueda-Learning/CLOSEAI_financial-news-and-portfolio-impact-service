# 模块 B/C 完整变更摘要（临时文档，合并前移除）

> 分支：`feature/module-bc-providers`
> 13 commits ahead of dev
> 基准：`项目15-⑧模块BC代码评审.md` + 后续补充决策
> 日期：2026-07-28 ~ 2026-07-29

---

## 一、新增功能

### Module B — 行情数据（报价刷新 + 收盘快照）

| 层 | 文件 | 职责 |
|----|------|------|
| integration/ | `PriceProvider.java` | 接口：fetchQuote + fetchDailyBars |
| integration/ | `QuoteSnapshot.java` | record：不带 JPA 的报价快照 |
| integration/ | `DailyBar.java` | record：日线 OHLC（B5 用） |
| integration/ | `ChainedPriceProvider.java` | 读路径链：Finnhub → TwelveData → DB |
| integration/ | `WritePriceProvider.java` | 写路径链：Finnhub → TwelveData（不含 DB） |
| integration/finnhub/ | `FinnhubQuoteResponse.java` | Finnhub `/quote` 响应 DTO |
| integration/finnhub/ | `FinnhubPriceProvider.java` | Finnhub 报价实现，@RateLimiter |
| integration/twelvedata/ | `TwelveDataQuoteResponse.java` | Twelve Data `/quote` 响应 DTO |
| integration/twelvedata/ | `TwelveDataTimeSeriesResponse.java` | Twelve Data `/time_series` 响应 DTO |
| integration/twelvedata/ | `TwelveDataPriceProvider.java` | Twelve Data 报价 + 日线实现 |
| integration/db/ | `DbPriceProvider.java` | 读路径兜底：从 price_quote 表读缓存 |
| integration/mock/ | `MockPriceProvider.java` | @ConditionalOnProperty("mock") 骨架 |
| repository/ | `PriceQuoteRepository.java` | JPA（模块A 加了 findBySymbolIn） |
| repository/ | `PricePointRepository.java` | JPA 复合主键 |
| repository/ | `PriceBarRepository.java` | JPA（B5 收盘快照用） |
| repository/ | `SecurityRepository.java` | watchlist 读取，A/B 共用 |
| service/ | `QuoteRefreshService.java` | 遍历 watchlist，注入写链 |
| service/ | `QuotePersistenceService.java` | 逐 symbol 事务持久化 |
| service/ | `ClosingSnapshotService.java` | B5 收盘快照：fetchDailyBars → price_bar |
| scheduler/ | `QuoteRefreshScheduler.java` | @Scheduled 1min + 交易时段 + 手动触发 |
| scheduler/ | `ClosingSnapshotScheduler.java` | @Scheduled cron 16:05 ET |
| api/internal/ | `QuoteRefreshController.java` | POST /api/v1/quotes/refresh |
| config/ | `RestClientConfig.java` | finnhub + twelvedata RestClient beans + Clock bean |

### Module C — 新闻聚合

| 层 | 文件 | 职责 |
|----|------|------|
| integration/ | `NewsProvider.java` | 接口：fetchCompanyNews |
| integration/ | `NewsItem.java` | record：含 headline/source/url/summary/image |
| integration/finnhub/ | `FinnhubNewsResponse.java` | Finnhub `/company-news` 响应 DTO |
| integration/finnhub/ | `FinnhubNewsProvider.java` | Finnhub 新闻实现，@RateLimiter |
| repository/ | `NewsArticleRepository.java` | JPA + findByExternalId（去重） |
| repository/ | `ArticleSecurityLinkRepository.java` | JPA 复合主键 |
| service/ | `NewsFetchService.java` | 遍历 watchlist，UTC 日期窗口 |
| service/ | `NewsPersistenceService.java` | 逐 article 事务 + 去重 + toEntity |
| scheduler/ | `NewsFetchScheduler.java` | @Scheduled 15min + 手动触发 |
| api/internal/ | `NewsFetchController.java` | POST /api/v1/news/fetch |

### 新增数据列

| Migration | 表 | 列 | 类型 |
|-----------|-----|-----|------|
| V8 | news_article | summary | TEXT NULL |
| V8 | article_security_link | match_method COMMENT | 更新为含 SEMANTIC_VECTOR |
| V9 | news_article | image | VARCHAR(1024) NULL |

### 其他

| 文件 | 改动 |
|------|------|
| `MatchMethod.java` | 新增 `SEMANTIC_VECTOR` 枚举值 |
| `application.yml` | twelvedata 配置段；provider 开关注释 |
| `.env.example` | TWELVEDATA_API_KEY；provider 开关注释 |
| `commitlint.config.js→.mjs` | ESM 格式 |
| `.github/workflows/ci.yml` | secrets 接入；nlp-test/frontend 注释 |

---

## 二、代码评审修复（基于 ⑧ 号评审文档）

### Fix 1: @Transactional 自调用绕过代理

- **问题**：`fetchAll()` 调 `this.fetchForSymbol()`，Spring AOP 不生效，article save + link save 在两个独立事务
- **修法**：拆出 `QuotePersistenceService` + `NewsPersistenceService`，通过注入调用 → @Transactional 生效
- **结果**：article + link 原子化。symbol 之间互不影响

### Fix 2: 执行锁返回值被丢弃

- **问题**：`doFetch()`/`doRefresh()` 里 `tryAcquire()` 返回值没接，拿不到锁也照跑；finally 无条件 release 会把别人持有的锁放掉
- **修法**：删除 doFetch/doRefresh，锁在 scheduled/manual 各自持。拿不到 → 定时跳过 / 手动 409
- **结果**：定时+手动不再并发，不会出现 uq_external_id 约束违反

### Fix 3: Twelve Data timestamp 使用 `timestamp` 字段

- **问题**：`datetime` 是纯日期 `"2026-07-28"` 无时间，`parseDatetime(dt+"Z")` 必然抛异常 → captured_at 全是 now()
- **修法**：改用 `r.timestamp()`（Unix UTC 秒），删掉 parseDatetime 方法
- **结果**：`Instant.ofEpochSecond(r.timestamp())`，和 Finnhub 完全一致

### Fix 4: 零值价格和 1970 时间戳

- **问题**：`r.c() == null` 不拦 BigDecimal.ZERO；`t` 是 long 原语，缺失填 0 → 1970-01-01
- **修法**：`signum() <= 0 → empty`；`t > 0` 再转 Instant
- **结果**：防腐层拦截上游零值，不泄漏到 service

### 中低优先级修复

| 条目 | 修复 |
|------|------|
| 3.1 写路径 DB 兜底 | 新建 WritePriceProvider（Finnhub→TwelveData only），QuoteRefreshService 用 @Qualifier 注入 |
| 3.3 时区 | NewsFetchService 用 `LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC)` |
| 3.4 MatchMethod 注释 | Javadoc 加 @see 标注 V2 列注释漂移；V8 修正 COMMENT |
| 3.5 Bean 注入 | 三个 Provider 构造函数 RestClient 参数统一 @Qualifier |
| 3.6 常量命名 | HEADLINE_MAX / SOURCE_MAX / URL_MAX UPPER_SNAKE_CASE，移到类顶部 |
| 3.7 orElse→orElseGet | QuotePersistenceService 使用 orElseGet |
| 3.8 summary | 落库不分析（D/E 确认），V8 migration + toEntity 映射 |
| 3.9 Provider 开关 | ChainedPriceProvider + MockPriceProvider 用 @ConditionalOnProperty 接线 |
| B5 收盘快照 | 收进本 PR：ClosingSnapshotService + ClosingSnapshotScheduler + TwelveData fetchDailyBars |

### 测试

| 文件 | 内容 |
|------|------|
| `QuoteRefreshSchedulerTest.java` | isTradingHours 6 个边界用例（开/收盘、周末、冬令时 EST） |

---

## 三、架构决策偏离记录

| 偏离 | 说明 |
|------|------|
| PriceProvider 返回 record 而非 Entity | ⑥ 号文档 §4 建议方案落地。`QuoteSnapshot`/`DailyBar`/`NewsItem` 均不带 JPA 注解，service 层做转换 |
| 写路径不含 DB 兜底 | 架构 §决策 1 的链式 Provider 保留给读路径；写路径使用独立 `WritePriceProvider`，两个上游全挂时日志如实反映 0 条新数据 |
| Twelve Data 时间来源用 `timestamp` 而非 `datetime` | API 实测证实 `datetime` 是纯日期，无时间部分。Unix `timestamp` 字段才是 UTC 绝对时间 |

---

## 四、合并前检查

- [ ] 本文档已删除
- [ ] V8 / V9 migration 序号与 dev 上最新不冲突
- [ ] `NewsArticle.summary` / `NewsArticle.image` 已告知 schema owner
- [ ] CI 绿灯（backend-lint / backend-type-check / backend-test / build）
- [ ] PR description 包含完整变更摘要

---

> ⚠️ 本文档为临时变更记录，**必须在合并到 dev 之前移除**。
