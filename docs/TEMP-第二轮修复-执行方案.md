# 第二轮修复 — 执行方案

> 基于同事代码审查 + E 模块接口需求
> 分支：feature/module-bc-providers
> 日期：2026-07-29

---

## Fix 1: 新闻手动刷新接口对齐契约

**修改**：`NewsFetchController.java` + `NewsFetchScheduler.java` + `NewsFetchService.java`

1. 路径：`/news/fetch` → `/news/refresh`
2. 返回体：202 空体 → 结构化 `{ inserted, skippedDuplicates }`
3. NewsFetchService.fetchAll() 返回统计数（区分 inserted / deduped）
4. NewsFetchScheduler.manualFetch() 返回 int
5. Controller 返回 200 + body（非冲突时）

---

## Fix 2: 去重逻辑支持多 symbol 链接

**修改**：`NewsPersistenceService.java`

逻辑改动：
- externalId 已存在 → 复用已有 article，不重复 insert
- 每个 symbol 独立检查 link 是否存在
- link 不存在 → 写新 link
- link 已存在 → 真正重复，skip

---

## Fix 3: 429 不重试

**修改**：`application.yml`

```yaml
retry.instances.externalApi:
    retry-exceptions: []   # 不重试任何异常 → 由 circuitbreaker 兜底
```

或者更精确：排除 4xx（429 属于 4xx）。Retry 只对 IOException / Timeout 生效。

选法：`retry-exceptions` 显式列出要重试的异常（SocketTimeoutException, ConnectTimeoutException），不列的不重试。

---

## Fix 4: EC-14 缺标题/时间跳过

**修改**：`FinnhubNewsProvider.java`

```java
return Arrays.stream(responses)
    .filter(r -> r.headline() != null && !r.headline().isBlank() && r.datetime() > 0)
    .map(r -> new NewsItem(...))
    .toList();
```

防腐层直接丢弃，不进 service。

---

## Fix 5: Mock 开关对写链路生效

**修改**：`WritePriceProvider.java`

加 `@ConditionalOnProperty(name = "app.providers.price", havingValue = "finnhub", matchIfMissing = true)`。

Mock 模式下 WritePriceProvider 不创建 → 需要一个 mock 版 write bean。

新建或复用 MockPriceProvider：加 `@Qualifier("writePriceProvider")` + `@ConditionalOnProperty(name = "app.providers.price", havingValue = "mock")`。

这样 `QuoteRefreshService` 和 `ClosingSnapshotService` 注入的 `@Qualifier("writePriceProvider")` 在 mock 模式下拿到 MockPriceProvider。

---

## Fix 6: 收盘快照抓当天

**修改**：`ClosingSnapshotService.java`

```java
LocalDate today = LocalDate.now();  // 删 minusDays(1)
```

---

## Fix 7: E 模块查询方法

**修改**：`NewsArticleRepository.java` + `ArticleSecurityLinkRepository.java`

```java
// NewsArticleRepository
List<NewsArticle> findByPublishedAtBetween(Instant start, Instant end);

// ArticleSecurityLinkRepository
List<ArticleSecurityLink> findByArticleId(Long articleId);
```

---

## 执行顺序

| 步 | 内容 | 文件数 | CI 关注 |
|----|------|:--:|------|
| 1 | Fix 7（E 模块方法） | 2 | backend-type-check |
| 2 | Fix 5（EC-14） | 1 | backend-type-check |
| 3 | Fix 3（去重） | 1 | backend-type-check |
| 4 | Fix 1（接口对齐） | 3 | backend-type-check |
| 5 | Fix 4（429） | 1 | backend-lint |
| 6 | Fix 6（mock 开关） | 2 | backend-type-check |
| 7 | Fix 2（收盘日期） | 1 | backend-type-check |
| 8 | commit + push | — | ALL GREEN |
