# FNPIS API 使用教程

> 版本 v1.0 ｜ 2026-07-29
> 所有接口 Base URL: `http://localhost:8080/api/v1`

---

## 目录

1. [快速开始](#快速开始)
2. [模块A — 组合与持仓](#模块a--组合与持仓)
3. [模块B — 行情报价](#模块b--行情报价)
4. [模块C — 新闻聚合](#模块c--新闻聚合)
5. [模块D/E — 情感与影响评估](#模块de--情感与影响评估)
6. [运维与演示](#运维与演示)
7. [错误码速查](#错误码速查)
8. [演示常用流程](#演示常用流程)

---

## 快速开始

### 在 Swagger 中测试

1. 打开 `http://localhost:8080/swagger-ui/index.html`
2. 找到对应分组（Portfolios / Holdings / Prices / News / Quotes / Impats）
3. 点 `Try it out` → 填参数 → `Execute`

### 用 curl 测试

```bash
# 以 GET 为例
curl -X GET 'http://localhost:8080/api/v1/portfolios' -H 'accept: application/json'
```

### 用 VS Code REST Client

打开 `docs/fnpis-api.http`，在请求上方点 "Send Request"。

---

## 模块A — 组合与持仓

### 1. 查看所有组合

```
GET /api/v1/portfolios
```

**Swagger**: Portfolios → `GET /api/v1/portfolios` → Execute

**curl**:
```bash
curl http://localhost:8080/api/v1/portfolios
```

**返回示例**:
```json
[
  {
    "id": 2,
    "name": "MVP测试账户2",
    "baseCurrency": "USD",
    "totalMarketValue": 0,
    "holdingCount": 3,
    "createdAt": "2026-07-29T09:42:16Z",
    "stale": false
  }
]
```

---

### 2. 创建组合

```
POST /api/v1/portfolios
```

**Swagger**: Portfolios → `POST /api/v1/portfolios` → Try it out → 改 body → Execute

**body**:
```json
{
  "name": "我的美股账户",
  "baseCurrency": "USD"
}
```

**返回**: 201 Created，返回新组合的完整信息

---

### 3. 查看组合估值汇总

```
GET /api/v1/portfolios/{id}/summary
```

**Swagger**: Portfolios → `GET /api/v1/portfolios/{id}/summary` → id 填 `2` → Execute

**curl**:
```bash
curl http://localhost:8080/api/v1/portfolios/2/summary
```

**返回示例**:
```json
{
  "id": 2,
  "name": "MVP测试账户2",
  "baseCurrency": "USD",
  "totalMarketValue": "0.00",
  "totalCost": "26550.00",
  "unrealizedPnL": "0.00",
  "allocations": [
    { "symbol": "AAPL", "marketValue": 0, "weight": 0 }
  ],
  "stale": true
}
```

> `stale: true` 表示没有实时报价数据（还没运行 quote refresh）

---

### 4. 删除组合

```
DELETE /api/v1/portfolios/{id}
```

**Swagger**: Portfolios → `DELETE /api/v1/portfolios/{id}` → id 填数字 → Execute

返回 204 No Content，级联删除所有持仓。

---

### 5. 查看持仓列表

```
GET /api/v1/portfolios/{portfolioId}/holdings
```

**Swagger**: Holdings → `GET /api/v1/portfolios/{portfolioId}/holdings` → portfolioId 填 `2`

**curl**:
```bash
curl http://localhost:8080/api/v1/portfolios/2/holdings
```

**返回示例**:
```json
{
  "content": [
    {
      "id": 1,
      "symbol": "AAPL",
      "companyName": "Apple Inc.",
      "quantity": "150",
      "costBasis": "177.00",
      "totalCost": "26550.00",
      "quoteAvailable": false
    }
  ],
  "page": 1,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1,
  "stale": true
}
```

> 分页参数：`?page=1&size=20`（默认）

---

### 6. 新增持仓

```
POST /api/v1/portfolios/{portfolioId}/holdings
```

**Swagger**: Holdings → `POST /api/v1/portfolios/{portfolioId}/holdings`

**正常新增**（新 symbol）:
```json
{
  "symbol": "MSFT",
  "quantity": 80,
  "costBasis": "420.00"
}
```
→ 返回 200，插入新行，`companyName` 自动从 security 表获取。

**重复合并测试（EC-09）**:
```json
{
  "symbol": "AAPL",
  "quantity": 50,
  "costBasis": "180.00"
}
```
→ 返回 200，AAPL 从 150 股合并为 200 股，costBasis 重算为加权均价 `(150×177 + 50×180) / 200 = 177.75`。

**无效 symbol（EC-05）**:
```json
{
  "symbol": "XYZQ",
  "quantity": 1,
  "costBasis": "1.00"
}
```
→ 返回 **404** `SECURITY_NOT_FOUND`。

---

### 7. 修改持仓

```
PATCH /api/v1/holdings/{id}
```

只传要改的字段：
```json
{
  "quantity": 200
}
```

---

### 8. 删除持仓

```
DELETE /api/v1/holdings/{id}
```

返回 204 No Content。

---

### 9. 搜索股票（新增持仓选择器）

```
GET /api/v1/securities?q={keyword}
```

**示例**:
```bash
curl "http://localhost:8080/api/v1/securities?q=AAPL"
```

**返回**:
```json
[
  { "symbol": "AAPL", "companyName": "Apple Inc." }
]
```

支持前缀模糊匹配：`q=A` 返回所有 A 开头的标的，`q=NV` 返回 NVDA。

---

## 模块B — 行情报价

### 10. 手动刷新报价

```
POST /api/v1/quotes/refresh
```

**Swagger**: Quotes → `POST /api/v1/quotes/refresh`

- 成功 → **202 Accepted**（空 body），遍历所有 watchlist symbol 拉取实时报价
- 已有刷新在跑 → **409 Conflict**

> 定时任务每分钟在美股交易时段（9:30-16:00 ET）自动跑。

**测试 409 并发锁**：快速连点两次 Execute，第一次 202，第二次应返回 409。

---

### 11. 获取最新报价

```
GET /api/v1/prices/{symbol}/quote
```

**Swagger**: Prices → `GET /api/v1/prices/{symbol}/quote`

**示例**：symbol 填 `AAPL`

```bash
curl http://localhost:8080/api/v1/prices/AAPL/quote
```

**返回示例**:
```json
{
  "symbol": "AAPL",
  "price": "340.0800",
  "previousClose": "336.9100",
  "change": "3.1700",
  "changePct": "0.94",
  "quoteAvailable": true,
  "asOf": "2026-07-28T20:00:00Z",
  "stale": true
}
```

| 字段 | 含义 |
|------|------|
| `price` | 当前价格 |
| `previousClose` | 前收盘价 |
| `change` | 涨跌额 |
| `changePct` | 涨跌幅（已×100） |
| `quoteAvailable` | `true`=有报价 / `false`=停牌或未拉取 |
| `asOf` | 数据采集时间（UTC） |
| `stale` | `true`=数据过期 |

**未知 symbol** → **404** `SECURITY_NOT_FOUND`：
```bash
curl http://localhost:8080/api/v1/prices/XYZQ/quote
```

---

### 12. 组合价值走势

```
GET /api/v1/portfolios/{portfolioId}/valuation-history
```

**参数**:
| 参数 | 默认 | 说明 |
|------|------|------|
| `from` | 30 天前 | 起始日期 `2026-07-01` |
| `to` | 今天 | 结束日期 `2026-07-29` |

**示例**:
```bash
curl "http://localhost:8080/api/v1/portfolios/2/valuation-history?from=2026-07-01&to=2026-07-29"
```

**返回**:
```json
{
  "portfolioId": 2,
  "points": [],
  "stale": true
}
```

> `points` 为空是因为收盘快照从未触发过——系统跳过了这部分数据记录。

---

## 模块C — 新闻聚合

### 13. 手动刷新新闻

```
POST /api/v1/news/refresh
```

**Swagger**: News → `POST /api/v1/news/refresh`

- 成功 → **200** OK，返回抓取统计
- 已有刷新在跑 → **409 Conflict**

**返回示例**:
```json
{
  "triggered": true,
  "fetched": 15,
  "inserted": 0,
  "skippedDuplicates": 2847,
  "failures": 0
}
```

| 字段 | 含义 |
|------|------|
| `triggered` | 是否接受执行 |
| `fetched` | 成功抓取新闻的 symbol 数 |
| `inserted` | 新入库的 article-link 行数 |
| `skippedDuplicates` | 已存在跳过的数量（去重生效） |
| `failures` | 持久化异常数 |

> 定时任务每 15 分钟自动跑，跳过非交易时段不跳过。

---

### 14. 新闻列表

```
GET /api/v1/news
```

**Swagger**: News → `GET /api/v1/news`

**筛选参数**:
| 参数 | 说明 | 示例 |
|------|------|------|
| `symbol` | 按股票代码筛选（C4） | `symbol=AAPL` |
| `sentiment` | 按情感筛选 | `sentiment=POSITIVE` |
| `from` | 起始日期 | `from=2026-07-20` |
| `to` | 结束日期 | `to=2026-07-29` |
| `page` | 页码（1-based） | `page=1` |
| `size` | 每页条数（默认20，上限100） | `size=5` |

**示例**:
```bash
# 全量列表
curl http://localhost:8080/api/v1/news

# 只看 AAPL 的前 5 条
curl "http://localhost:8080/api/v1/news?symbol=AAPL&size=5"

# 只看 POSITIVE 情感的新闻
curl "http://localhost:8080/api/v1/news?sentiment=POSITIVE"

# 日期范围筛选
curl "http://localhost:8080/api/v1/news?from=2026-07-25&to=2026-07-29"
```

**返回示例**（一条记录）:
```json
{
  "content": [
    {
      "id": 1930,
      "headline": "Ben Cherniawski, Ph.D., Named Director of Production...",
      "source": "Yahoo",
      "url": "https://finnhub.io/api/news?id=e9550...",
      "publishedAt": "2026-07-29T07:01:00Z",
      "symbols": ["AAPL"],
      "sentiment": null,
      "hasImpact": false
    }
  ],
  "page": 1,
  "size": 20,
  "totalElements": 1928,
  "totalPages": 97,
  "stale": false
}
```

| 字段 | 含义 |
|------|------|
| `symbols` | 数组——一条新闻可关联多只股票（EC-24） |
| `sentiment` | `null`=情感未完成分析；否则含 `label`/`score`/`confidence` |
| `hasImpact` | 是否已有影响评估（可点击进联动视图） |

---

### 15. 新闻详情

```
GET /api/v1/news/{id}
```

**Swagger**: News → `GET /api/v1/news/{id}`

**示例**:
```bash
curl http://localhost:8080/api/v1/news/1
```

**返回示例**:
```json
{
  "id": 1,
  "headline": "eight Telecom marks annual eight Day celebrations...",
  "source": "Yahoo",
  "url": "https://finnhub.io/api/news?id=b37d5...",
  "summary": "eight Telecom (powered by StarHub) is marking eight Day...",
  "image": "https://s.yimg.com/rz/stage/p/yahoo_finance_en-US_h_p_finance_2.png",
  "publishedAt": "2026-07-29T05:41:00Z",
  "fetchedAt": "2026-07-29T07:25:38.554Z",
  "symbols": ["AAPL"],
  "sentiment": null,
  "hasImpact": false
}
```

**不存在的 ID** → **404** `ARTICLE_NOT_FOUND`：
```bash
curl http://localhost:8080/api/v1/news/99999
```

---

## 模块D/E — 情感与影响评估

### 16. 手动触发情感分析

```
POST /api/v1/sentiment/refresh
```

**Swagger**: Sentiment → `POST /api/v1/sentiment/refresh`

对所有未分析的新闻调用 LLM 进行情感分类。已有情感分的跳过（`sentiment_score.article_id` UNIQUE）。

---

### 17. 影响评估列表

```
GET /api/v1/portfolios/{portfolioId}/impacts
```

**Swagger**: Impacts → `GET /api/v1/portfolios/{portfolioId}/impacts`

查 `impact_assessment` 表，按日期筛选。

**示例**:
```bash
curl "http://localhost:8080/api/v1/portfolios/2/impacts?date=2026-07-29"
```

---

### 18. 影响评估汇总

```
GET /api/v1/portfolios/{portfolioId}/impact-summary
```

日度汇总：方向一致率、样本数、top impacted 排行榜。

```bash
curl "http://localhost:8080/api/v1/portfolios/2/impact-summary?date=2026-07-29"
```

---

### 19. 联动视图（演示核心）

```
GET /api/v1/news/{id}/impact-view?portfolioId={pid}&symbol={sym}
```

**Swagger**: ImpactViewController → `GET /api/v1/news/{id}/impact-view`

**参数**:
| 参数 | 必填 | 说明 |
|------|:--:|------|
| `id` | 是 | 新闻 ID |
| `portfolioId` | 是 | 组合 ID |
| `symbol` | 否 | 指定价格曲线标的，缺省取影响金额最大的 |

**示例**:
```bash
curl "http://localhost:8080/api/v1/news/1/impact-view?portfolioId=2"
```

**返回结构**（有数据时）:
```json
{
  "article": { "...": "新闻详情" },
  "attributionDate": "2026-07-29",
  "impactedSymbols": ["AAPL", "NVDA"],
  "selectedSymbol": "AAPL",
  "impacts": [
    {
      "symbol": "AAPL",
      "holdingWeight": 0.328,
      "priceChangePct": 4.15,
      "expectedImpact": 0.192,
      "observedContribution": 1.258,
      "valueImpact": "1614.36",
      "direction": "POSITIVE",
      "alignment": "CONFIRMED"
    }
  ],
  "priceSeries": {
    "symbol": "AAPL",
    "previousClose": "121.40",
    "newsMarker": "2026-07-27T12:31:00Z",
    "points": [
      { "t": "2026-07-27T12:00:00Z", "price": "121.85" },
      { "t": "2026-07-27T12:31:00Z", "price": "121.90" }
    ]
  }
}
```

---

## 运维与演示

### 20. 触发影响重算（需管理员令牌）

```
POST /api/v1/impacts/recompute
```

Header: `X-Admin-Token: {your_token}`
Body:
```json
{
  "portfolioId": 2,
  "date": "2026-07-29"
}
```

> 默认关闭（`ADMIN_ENABLED=false`）。启用需设环境变量 `ADMIN_ENABLED=true` + `ADMIN_TOKEN`。

---

## 错误码速查

| HTTP | code | 触发场景 |
|------|------|----------|
| 400 | `VALIDATION_FAILED` | 请求字段校验失败 |
| 404 | `PORTFOLIO_NOT_FOUND` | 组合不存在 |
| 404 | `HOLDING_NOT_FOUND` | 持仓不存在 |
| 404 | `SECURITY_NOT_FOUND` | 股票代码不在 watchlist |
| 404 | `ARTICLE_NOT_FOUND` | 新闻不存在 |
| 404 | `ENDPOINT_NOT_FOUND` | admin 功能未启用 |
| 403 | `ADMIN_TOKEN_INVALID` | admin token 错误 |
| 409 | `TASK_ALREADY_RUNNING` | 刷新/重算正在执行 |
| 503 | `UPSTREAM_UNAVAILABLE` | 外部数据源不可用且无缓存 |

---

## 演示常用流程

### 造数据

```bash
# 1. 创建组合
curl -X POST http://localhost:8080/api/v1/portfolios \
  -H 'Content-Type: application/json' \
  -d '{"name":"演示组合"}'

# 2. 加几个持仓（假设 portfolioId=2）
for symbol in AAPL TSLA NVDA MSFT; do
  curl -X POST "http://localhost:8080/api/v1/portfolios/2/holdings" \
    -H 'Content-Type: application/json' \
    -d "{\"symbol\":\"$symbol\",\"quantity\":100,\"costBasis\":\"200.00\"}"
done

# 3. 拉行情
curl -X POST http://localhost:8080/api/v1/quotes/refresh

# 4. 拉新闻（可能需要几十秒）
curl -X POST http://localhost:8080/api/v1/news/refresh

# 5. 跑情感分析
curl -X POST http://localhost:8080/api/v1/sentiment/refresh

# 6. 打开联动视图查看效果
curl "http://localhost:8080/api/v1/news/1/impact-view?portfolioId=2"
```

### 演示降级

```bash
# 模拟上游挂掉 — 读接口仍返回旧数据 + stale:true
curl "http://localhost:8080/api/v1/prices/AAPL/quote"
```

### 并发锁验证

```bash
# 快速连发两次，第二次应返回 409
curl -X POST http://localhost:8080/api/v1/news/refresh &
curl -X POST http://localhost:8080/api/v1/news/refresh
```
