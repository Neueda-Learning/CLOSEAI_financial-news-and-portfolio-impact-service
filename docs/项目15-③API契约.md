# 项目 15 API 契约
## Financial News & Portfolio Impact Service

> 版本 v0.4（草案 —— 六个架构决策未全部拍板前先行产出，供前端开工）
> 前置文档：`项目15-①需求文档.md` v2.4、`项目15-②架构设计.md` v1.5
> 状态：**未定稿，但前端可以据此直接开工**。标 ⚠️ 的地方是决策未定、后续可能变化的点。

> v0.4 变更：`securities?q=` 的匹配规则定案（代码前缀 + 公司名包含），
> 补上响应示例；§9 待定项相应划掉。实现细节见 `项目15-⑧` §3。

> v0.3 变更：情感分析定为 LLM Agent 单一方案 —— `sentiment.engine` 字段删除，
> 明确 `sentiment: null` 的两种成因（分析中／校验失败）。

> v0.2 变更（据评审意见）：联动视图新增 `selectedSymbol` / `impactedSymbols`
> 消除曲线归属歧义；错误响应骨架统一，`errors` 改为附加字段；
> 管理端点补 profile 门禁、`X-Admin-Token`、TTL 自动失效与审计字段；
> 新增唯一真源声明并同步两份 README。

---

## ⚠️ 唯一真源声明

**本文档是 API 的唯一真源。**

仓库 `ReadMe.md` / `ReadMe.zh-CN.md` 里的 REST API 章节是早期草稿，
端点形状与本文档**不一致**（无 `/v1` 前缀、用 `PUT` 而非 `PATCH`、
`ticker` 而非 `symbol`、有 `/dashboard/**` 一组本文档已合并掉的端点）。

**按 README 开发会在联调时出现大面积 404 和字段不匹配。**
README 相关章节已加醒目提示指向本文档，但请直接以本文档为准。

优先级：**Swagger（实现后）> 本文档 > README**。
后端实现完成后，若 Swagger 与本文档冲突，先判断是实现偏离契约还是契约需要更新，
不要默认代码是对的。

---

## 使用说明

接口形状主要由需求文档的用户故事和页面（F1~F5）决定，架构那六个决策里
只有三个会影响接口，其余三个（Provider 接口、新闻精确匹配、日线表）是纯内部实现，
前端完全看不见，不用等它们定案。

会影响接口的三处，已在文档里用 ⚠️ 标出：

| 决策 | 影响哪里 | 当前假设 | 如果决策改变会怎样 |
|---|---|---|---|
| 决策2 缓存优先 | 几乎所有响应体的 `asOf`/`stale` 字段 | 保留，见 §1.3 | 若不采用，删掉这两个字段，其余结构不变 |
| 决策3 影响落库 | `/impacts/recompute` 端点是否存在 | 存在（演示手动触发用） | 若改现算，端点删除，其他响应结构不变 |
| ~~决策5 引擎策略化~~ | ~~sentiment 对象的 `engine`~~ | **已定案：单一 Agent 方案** | `engine` 字段已删除，只保留 `modelVersion` |

前端现在就可以按本文档的响应示例直接写 mock（建议 MSW 或 json-server），
等真实后端上线只需换 baseURL，字段名已经和后端约定好，不需要重写页面逻辑。

---

## 目录

1. 通用约定
2. 端点总览
3. 组合与持仓
4. 标的与行情
5. 新闻
6. 情感与影响评估（核心）
7. 运维与演示辅助
8. 对外只读 API
9. 错误响应格式与错误码
10. 前端集成清单
11. Swagger / OpenAPI 生成方式

---

# 1. 通用约定

## 1.1 基础

| 项 | 约定 |
|---|---|
| 内部接口前缀 | `/api/v1`（前端专用，无鉴权，单用户系统） |
| 对外只读前缀 | `/public/v1`（讲师用，API Key 鉴权） |
| 内容类型 | `application/json; charset=utf-8` |
| 字符编码 | UTF-8 |
| 接口文档 | `/swagger-ui`（内部与对外分两个 group） |

## 1.2 数据类型约定（重要，前端必读）

| 类型 | 传输格式 | 前端怎么处理 |
|---|---|---|
| **金额、价格、数量** | **字符串** `"128450.75"` | **不要用 `Number()` 参与运算**，只做展示；需计算时用 decimal.js 一类库 |
| 百分比 | 数字 `7.94` 表示 7.94% | 直接显示，已乘过 100 |
| 比率、权重 | 数字 `0.303` 表示 30.3% | 显示时自己乘 100 |
| 情感分值 score | 数字 `0.72`，范围 -1 ~ 1 | — |
| 时间戳 | ISO-8601 UTC `"2026-07-27T12:31:00Z"` | 展示时转本地时区 |
| 日期（交易日） | `"2026-07-27"` | 不带时区 |
| 股票代码 | 大写字符串 `"NVDA"` | — |

> **为什么金额是字符串**：JavaScript 的 Number 是双精度浮点，
> `0.1 + 0.2 = 0.30000000000000004`。演示时讲师会手工验算总市值（SC-002），
> 前端做浮点运算就会对不上。后端已按字符串序列化，前端保持原样展示即可。

## 1.3 ⚠️ 数据新鲜度字段（依赖决策 2）

所有**包含外部数据**（行情、新闻）的响应都带这两个字段：

```json
{
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

| 字段 | 含义 |
|---|---|
| `asOf` | 这份数据的采集时间，不是请求时间 |
| `stale` | 后端判定数据是否已过期（阈值由后端配置） |

**前端必须把这两个字段展示出来**，例如「数据更新于 3 分钟前」，
`stale: true` 时加视觉标记（灰化或黄色角标）。

这不是可选的锦上添花 —— 需求 B4 和成功标准 SC-009 都要求它，
演示时外部数据源宕机后靠这个证明系统在诚实降级。

纯自有数据的接口（组合列表、持仓 CRUD）`asOf` 可能为 `null`，前端按可空处理。

## 1.4 分页

列表接口统一用这套参数与响应结构：

| 参数 | 默认 | 说明 |
|---|---|---|
| `page` | 1 | **页码从 1 开始** |
| `size` | 20 | 每页条数，上限 100 |
| `sort` | 各接口自定 | 形如 `publishedAt,desc` |

```json
{
  "content": [ ... ],
  "page": 1,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

---

# 2. 端点总览

优先级与需求文档一致。**前端按 P0 → P1 → P2 顺序做即可。**

## 组合与持仓

| 方法 | 路径 | 说明 | 需求 | 优先级 |
|---|---|---|---|---|
| GET | `/api/v1/portfolios` | 组合列表 | A2 | P0 |
| POST | `/api/v1/portfolios` | 创建组合 | A1 | P0 |
| GET | `/api/v1/portfolios/{id}` | 组合详情 | A2 | P0 |
| DELETE | `/api/v1/portfolios/{id}` | 删除组合（级联删持仓） | A3 | P0 |
| GET | `/api/v1/portfolios/{id}/summary` | **估值汇总 + 配置分布** | B2,B3 | P0 |
| GET | `/api/v1/portfolios/{id}/holdings` | 持仓列表（含市值盈亏） | A5 | P0 |
| POST | `/api/v1/portfolios/{id}/holdings` | 新增持仓 | A4 | P0 |
| PATCH | `/api/v1/holdings/{id}` | 修改数量／成本价 | A7 | P1 |
| DELETE | `/api/v1/holdings/{id}` | 删除持仓 | A6 | P0 |
| GET | `/api/v1/portfolios/{id}/valuation-history` | 组合价值走势 | B5,F5 | P1 |

## 标的与行情

| 方法 | 路径 | 说明 | 需求 | 优先级 |
|---|---|---|---|---|
| GET | `/api/v1/securities?q=` | 标的搜索（新增持仓时用）**已实现，匹配规则见 §4** | A4,EC-05 | P1 |
| GET | `/api/v1/prices/{symbol}/quote` | 最新报价 | B1 | P0 |
| GET | `/api/v1/prices/{symbol}/history` | 历史日线 | B5 | P1 |

## 新闻

| 方法 | 路径 | 说明 | 需求 | 优先级 |
|---|---|---|---|---|
| GET | `/api/v1/news` | 新闻列表（可按 symbol／情感筛选） | C3,C4 | P0 |
| GET | `/api/v1/news/{id}` | 新闻详情含情感 | C5 | P0 |
| POST | `/api/v1/news/refresh` | 手动刷新（演示用） | C6 | P1 |
| POST | `/api/v1/quotes/refresh` | 手动刷新报价（演示用） | B6 | P1 |

## 影响评估（核心）

| 方法 | 路径 | 说明 | 需求 | 优先级 |
|---|---|---|---|---|
| GET | `/api/v1/news/{id}/impact-view` | **联动视图一次取全**（演示 Hook） | F4,E1~E4 | P1 |
| GET | `/api/v1/portfolios/{id}/impacts` | 影响列表，可筛选 | E1~E3 | P1 |
| GET | `/api/v1/portfolios/{id}/impact-summary` | 日度汇总（方向一致率） | E5 | P1 |
| POST | `/api/v1/impacts/recompute` | ⚠️ 重算（依赖决策 3） | — | P1 |

## 运维与演示

| 方法 | 路径 | 说明 | 需求 | 优先级 |
|---|---|---|---|---|
| GET | `/api/v1/providers/status` | 数据源健康与额度 | EC-12 | P2 |
| POST | `/api/v1/admin/providers/{name}/simulate-outage` | 宕机演示开关 | SC-009 | P2 |
| GET | `/actuator/health` | 健康检查（Spring 自带） | — | P0 |

## 各页面依赖哪些接口

| 页面 | 依赖端点 |
|---|---|
| F1 组合总览 | `/portfolios`、`/portfolios/{id}/summary` |
| F2 持仓管理 | `/portfolios/{id}/holdings`、`/securities?q=`、POST/PATCH/DELETE holdings |
| F3 新闻流 | `/news`、`/news/{id}` |
| F4 联动视图 | **`/news/{id}/impact-view` 一个就够** |
| F5 价值走势 | `/portfolios/{id}/valuation-history` |

---

# 3. 请求体与校验规则

校验规则直接对应需求文档第 8 章边界条件，前端可以照此提前写表单校验。

## POST /api/v1/portfolios

```json
{ "name": "美股账户", "baseCurrency": "USD" }
```

| 字段 | 规则 | 对应 EC |
|---|---|---|
| name | 必填，1~100 字符 | EC-10 |
| baseCurrency | 固定 "USD"（MVP 唯一值） | AS-02 |

## POST /api/v1/portfolios/{id}/holdings

```json
{ "symbol": "NVDA", "quantity": 20, "costBasis": "100.00" }
```

| 字段 | 规则 | 对应 EC |
|---|---|---|
| symbol | 必须存在于 security 表，否则 404 | EC-05 |
| quantity | 正整数，MVP 不接受小数股 | EC-06,EC-08 |
| costBasis | ≥ 0，允许 0（赠股场景） | EC-07 |

同一组合重复添加同一代码：**后端合并为一笔并重算加权均价**，不报错，返回合并后的持仓（EC-09）。

## PATCH /api/v1/holdings/{id}

```json
{ "quantity": 25, "costBasis": "105.00" }
```

两个字段都可选，传哪个改哪个。

---

# 4. 关键响应示例

## GET /api/v1/portfolios/{id}/summary

```json
{
  "id": 1,
  "name": "美股账户",
  "baseCurrency": "USD",
  "totalMarketValue": "128450.75",
  "totalCost": "119000.00",
  "unrealizedPnL": "9450.75",
  "unrealizedPnLPct": 7.94,
  "dayChange": "-1820.40",
  "dayChangePct": -1.40,
  "allocations": [
    { "symbol": "AAPL", "marketValue": "42150.00", "weight": 0.328 },
    { "symbol": "NVDA", "marketValue": "38900.25", "weight": 0.303 }
  ],
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

组合里没有任何持仓时（EC-01）：`totalMarketValue` 为 `"0.00"`，`allocations` 为空数组，
前端据此显示空状态提示，不当作错误处理。

## GET /api/v1/portfolios/{id}/holdings

```json
{
  "content": [
    {
      "id": 91,
      "symbol": "NVDA",
      "companyName": "NVIDIA Corporation",
      "quantity": "20",
      "costBasis": "100.00",
      "currentPrice": "125.60",
      "previousClose": "121.40",
      "marketValue": "2512.00",
      "totalCost": "2000.00",
      "unrealizedPnL": "512.00",
      "unrealizedPnLPct": 25.60,
      "dayChange": "84.00",
      "dayChangePct": 3.46,
      "weight": 0.303,
      "quoteAvailable": true
    }
  ],
  "page": 1, "size": 20, "totalElements": 3, "totalPages": 1,
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

两个前端要处理的边界：

- `costBasis` 为 `"0.00"` 时（EC-07），`unrealizedPnLPct` 为 **`null`**，
  前端显示 `—`，不要显示 `Infinity` 或 `NaN`
- 停牌查不到报价时（EC-13），`quoteAvailable` 为 `false`，
  `currentPrice` 是最后已知价，前端加「无实时报价」标记

## GET /api/v1/news

查询参数：

| 参数 | 说明 |
|---|---|
| `symbol` | 按代码筛选（C4） |
| `sentiment` | `POSITIVE` / `NEGATIVE` / `NEUTRAL` |
| `from` / `to` | 发布时间范围 |
| `page` / `size` / `sort` | 默认 `publishedAt,desc`，每页 20 条（C3） |

```json
{
  "content": [
    {
      "id": 8842,
      "headline": "Nvidia beats Q2 estimates, raises guidance",
      "source": "Reuters",
      "url": "https://example.com/article/8842",
      "publishedAt": "2026-07-27T12:31:00Z",
      "symbols": ["NVDA"],
      "sentiment": {
        "label": "POSITIVE",
        "score": 0.72,
        "confidence": 0.88,
        "modelVersion": "agent-v1"
      },
      "hasImpact": true
    }
  ],
  "page": 1, "size": 20, "totalElements": 137, "totalPages": 7,
  "asOf": "2026-07-27T15:40:00Z",
  "stale": false
}
```

- `symbols` 是**数组**，一条新闻可关联多只股票（EC-24），前端不要假设只有一个
- `modelVersion` 仅用于追溯，前端不需要展示
- **`sentiment` 为 `null` 有两种情况**，前端都要处理：
  尚未分析完 → 显示「分析中」；LLM 返回结果校验不通过 → 显示「分析失败」。
  两者都不是错误响应，不要弹错误提示。情感分析走 LLM,比新闻入库慢,
  **列表里出现 `sentiment: null` 是常态,不是异常**
- `hasImpact` 告诉前端这条新闻能不能点进联动视图

## GET /api/v1/news/{id}/impact-view ★ 联动视图

**这是整个项目最重要的一个接口**，对应需求 F4 和演示脚本第 4 步。
设计成一次请求取全所有数据，前端不需要拼接多个接口。

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `portfolioId` | 是 | 影响金额依赖具体组合的持仓权重 |
| `symbol` | 否 | 指定要返回哪只股票的价格曲线；缺省时取 `impacts` 中 `valueImpact` 绝对值最大的那只 |

```json
{
  "article": {
    "id": 8842,
    "headline": "Nvidia beats Q2 estimates, raises guidance",
    "source": "Reuters",
    "url": "https://example.com/article/8842",
    "publishedAt": "2026-07-27T12:31:00Z",
    "sentiment": {
      "label": "POSITIVE", "score": 0.72, "confidence": 0.88,
      "modelVersion": "agent-v1"
    }
  },
  "attributionDate": "2026-07-27",
  "impactedSymbols": ["NVDA", "AMD"],
  "selectedSymbol": "NVDA",
  "impacts": [
    {
      "symbol": "NVDA",
      "companyName": "NVIDIA Corporation",
      "holdingWeight": 0.303,
      "priceChangePct": 4.15,
      "expectedImpact": 0.192,
      "observedContribution": 1.258,
      "valueImpact": "1614.36",
      "direction": "POSITIVE",
      "alignment": "CONFIRMED"
    },
    {
      "symbol": "AMD",
      "companyName": "Advanced Micro Devices",
      "holdingWeight": 0.120,
      "priceChangePct": 2.80,
      "expectedImpact": 0.077,
      "observedContribution": 0.336,
      "valueImpact": "508.80",
      "direction": "POSITIVE",
      "alignment": "CONFIRMED"
    }
  ],
  "priceSeries": {
    "symbol": "NVDA",
    "previousClose": "121.40",
    "newsMarker": "2026-07-27T12:31:00Z",
    "points": [
      { "t": "2026-07-27T12:00:00Z", "price": "121.85" },
      { "t": "2026-07-27T12:31:00Z", "price": "121.90" },
      { "t": "2026-07-27T12:35:00Z", "price": "125.60" }
    ]
  },
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

**多标的与价格曲线的对应关系（重要，别搞错）**

`impacts` 是数组（可能多条），但 `priceSeries` 只有一条。
两者靠 `selectedSymbol` 显式绑定：

| 字段 | 含义 |
|---|---|
| `impactedSymbols` | 这条新闻影响到的全部代码，用于渲染 tab／切换器 |
| `selectedSymbol` | **当前 `priceSeries` 属于哪只股票**，后端保证 `priceSeries.symbol` 与它一致 |
| `impacts[]` | 全部受影响持仓的评估结果，每条都要渲染 |

前端交互：用 `impactedSymbols` 渲染切换器，点击某只时带 `?symbol=` 重新请求，
拿回该股票的曲线。**不要靠 `impacts[0]` 猜曲线归属** —— 那是这个接口最容易踩的坑。

> 为什么不一次返回所有股票的曲线：日内价格点要按代码分别取，
> 一次返回 N 条曲线会成倍消耗 Finnhub 额度，而用户同一时刻只看一条。
> 需要并排对比多只时再议，MVP 不做。

**其余要点：**

`priceSeries.newsMarker` 就是 Chart.js annotation 插件那条竖线的 x 值，
不需要前端从 `publishedAt` 二次推算 —— 后端已经对齐到价格序列的时间轴上了。

`alignment` 三种值决定展示样式：

| 值 | 含义 | 建议展示 |
|---|---|---|
| `CONFIRMED` | 新闻方向与股价一致 | ✓ 绿色「方向验证成功」 |
| `DIVERGENT` | 方向相反 | ✗ 橙色「与预期相反」 |
| `INCONCLUSIVE` | 波动过小或缺数据 | — 灰色「无法判断」 |

`priceSeries.points` 可能只有很少的点（免费数据源限制），
前端要能优雅处理点数少的情况，不要因为点数不足就画不出图。

## GET /api/v1/portfolios/{id}/impact-summary

查询参数：`date`（默认今天）

```json
{
  "date": "2026-07-27",
  "weightedSentiment": 0.34,
  "newsCoverage": 0.60,
  "directionAgreementRate": 0.71,
  "sampleSize": 24,
  "counts": { "confirmed": 5, "divergent": 2, "inconclusive": 4 },
  "topImpacted": [
    { "symbol": "NVDA", "valueImpact": "1614.36", "alignment": "CONFIRMED" },
    { "symbol": "TSLA", "valueImpact": "-892.10", "alignment": "DIVERGENT" }
  ],
  "asOf": "2026-07-27T16:05:00Z",
  "stale": false
}
```

`directionAgreementRate` 就是演示脚本第 5 步要展示的「方向一致率」。
**`sampleSize` 必须一起显示** —— 成功标准 SC-008 要求这个指标基于不少于 20 条记录，
只显示 71% 不显示样本数,讲师问起来就说不清。

样本不足时 `directionAgreementRate` 为 `null`，前端显示「样本不足」。

## GET /api/v1/portfolios/{id}/valuation-history

查询参数：`from` / `to`（默认最近 30 个交易日）

```json
{
  "portfolioId": 1,
  "points": [
    { "date": "2026-07-25", "totalValue": "127300.10" },
    { "date": "2026-07-26", "totalValue": "130271.15" },
    { "date": "2026-07-27", "totalValue": "128450.75" }
  ],
  "asOf": "2026-07-27T16:05:00Z",
  "stale": false
}
```

> ⚠️ **这个接口初期可能返回空数组。** 组合价值快照是每天收盘后累积的，
> 系统刚上线时没有历史数据（架构文档 §6.3 的日线兜底方案更是要跑几天才有数据）。
> **前端必须处理 `points` 为空的情况**，显示「数据积累中」而不是空白图表或报错。

## GET /api/v1/securities?q=（标的搜索，已实现）

新增持仓表单的代码选择器。`q` 可选。

```json
[
  { "symbol": "NVDA", "companyName": "NVIDIA Corporation" }
]
```

**裸数组，不是分页信封。** 这是下拉建议，不翻页；后端封顶 20 条。

**没有 `asOf` / `stale`。** `security` 表由 V5 迁移预置、不经过任何 provider，符合 §1.3 里「纯自有数据」的例外。

匹配规则（原 §9 待定项，现已定案）：

| 列 | 规则 | 例 |
|---|---|---|
| `symbol` | 前缀匹配 | `nv` → NVDA；`vd` → 无 |
| `companyName` | 包含匹配 | `disney` → DIS |

两列规则不同是有意的：代码短且从头输入，包含匹配会让单字符查询命中大半张表；公司名相反，没人从头拼全称。大小写不敏感。

- **`q` 缺省或为空 → 返回全部自选股**，不是空数组。用户还没输入时最需要知道系统接受哪些代码
- **查不到 → 200 加空数组**，不是 404。404 留给 `POST /holdings` 提交了不存在的代码（EC-05）
- 查询串里的 `%`、`_` 被转义，不会当通配符

## GET /api/v1/prices/{symbol}/quote

```json
{
  "symbol": "NVDA",
  "price": "125.60",
  "previousClose": "121.40",
  "change": "4.20",
  "changePct": 3.46,
  "quoteAvailable": true,
  "asOf": "2026-07-27T15:42:10Z",
  "stale": false
}
```

## POST /api/v1/news/refresh

演示用的手动刷新（C6）。无请求体。

```json
{ "triggered": true, "fetched": 12, "inserted": 3, "skippedDuplicates": 9 }
```

**已有刷新任务在跑时返回 `409 Conflict`**（EC-20），前端据此提示「正在刷新中」，
不要静默无反应 —— 演示时静默比报错更尴尬。

连续点两次，第二次 `inserted` 应为 0（SC-005 的去重验收点）。

## POST /api/v1/quotes/refresh

演示用的手动刷新报价（B6）。无请求体。

**成功**返回 `202 Accepted`，空 body。报价刷新由调度器异步执行，
逐 symbol 拉取最新价并写入 `price_quote` + `price_point` 表。

**已有刷新任务在跑时返回 `409 Conflict`**（EC-20），与 `/news/refresh` 一致。
前端据此提示「报价刷新正在进行中」。

## GET /api/v1/providers/status（P2，运维页 + 演示用）

```json
{
  "providers": [
    { "name": "finnhub-news",  "healthy": true,  "lastSuccessAt": "2026-07-27T15:40:00Z", "rateLimited": false },
    { "name": "finnhub-price", "healthy": true,  "lastSuccessAt": "2026-07-27T15:42:10Z", "rateLimited": false }
  ]
}
```

两个 provider 分开列，因为新闻和行情用的是不同账号的 key（架构文档 §6.5），
额度是独立的 —— 新闻被限流时行情可能仍然正常。

## 管理端点的安全边界（先读这段再看端点）

`/api/v1/admin/**` 和 `/api/v1/impacts/recompute` 是**写操作且影响全局行为**，
不能和普通读接口同等对待。三条硬性要求：

| 要求 | 做法 |
|---|---|
| **1. 只在非生产 profile 启用** | `@Profile("!prod")` 或 `@ConditionalOnProperty(app.admin.enabled=true)`，默认关闭 |
| **2. 需要管理头** | 请求头 `X-Admin-Token`，值来自环境变量；缺失或错误返回 `403` |
| **3. 操作可审计** | 响应回带操作者、生效时间、自动失效时间 |

关闭时访问返回 `404`（不是 403）—— 不暴露端点存在与否。
**对外只读 API `/public/v1/**` 永不包含任何管理端点。**

> 演示当天用的是非生产 profile，这些端点是开着的 —— 这正是设计意图。
> 加这层边界不是为了防谁，而是为了**避免有人在部署脚本里误开到长期运行的环境上**，
> 以及万一 `X-Admin-Token` 忘配时能明确报错而不是静默生效。

## POST /api/v1/admin/providers/{name}/simulate-outage（P2，宕机演示）

请求头：`X-Admin-Token: <token>`（必填）

```json
{ "enabled": true, "ttlSeconds": 300 }
```

`name` 取 `finnhub-news` / `finnhub-price`。开启后对应 Provider 立即返回失败，
页面应当照常显示缓存数据并把 `stale` 置为 `true`。这是演示脚本第 6 步。

响应：

```json
{
  "provider": "finnhub-price",
  "enabled": true,
  "triggeredBy": "admin-token:demo",
  "effectiveAt": "2026-07-27T16:10:00Z",
  "expiresAt": "2026-07-27T16:15:00Z"
}
```

**`ttlSeconds` 到期后自动恢复**，默认 300 秒，上限 3600。
这条是有意设计的：演示时最怕的是开了降级模式忘记关，
后面所有页面一直显示过期数据还以为是 bug。到期自动恢复消除了这个失误。

## POST /api/v1/impacts/recompute（⚠️ 依赖决策 3）

请求头：`X-Admin-Token: <token>`（必填）
请求体：`{ "portfolioId": 1, "date": "2026-07-27" }`

```json
{
  "recomputed": 11,
  "portfolioId": 1,
  "date": "2026-07-27",
  "triggeredBy": "admin-token:demo",
  "completedAt": "2026-07-27T16:12:03Z"
}
```

已有重算在跑时返回 `409 TASK_ALREADY_RUNNING`（EC-20）。
**这个端点会覆写当日已有的评估结果**，属于破坏性操作，所以同样要过管理头。

---

# 5. 对外只读 API

鉴权：请求头 `X-API-Key: <key>`。缺失或错误返回 `401`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/public/v1/portfolios` | 组合列表 |
| GET | `/public/v1/portfolios/{id}/summary` | 组合估值快照 |
| GET | `/public/v1/portfolios/{id}/holdings` | 持仓明细 |
| GET | `/public/v1/portfolios/{id}/impacts/latest` | 最近影响评估 |
| GET | `/public/v1/news/latest?symbol=` | 最新新闻含情感 |

响应结构与内部同名接口一致，**只读，无写操作**。独立限流，独立 Swagger group。
演示时把地址和 key 写在 slide 上（需求 G1~G3、成功标准 SC-010）。

---

# 6. 错误响应格式

统一用 RFC 7807 `application/problem+json`：

```json
{
  "type": "https://api.example.com/errors/security-not-found",
  "title": "Security not found",
  "status": 404,
  "detail": "Symbol 'XYZQ' does not exist",
  "instance": "/api/v1/portfolios/1/holdings",
  "code": "SECURITY_NOT_FOUND"
}
```

前端按 `code` 字段做判断，不要解析 `detail` 文案（文案会变）。

| HTTP | code | 触发场景 | 对应 EC |
|---|---|---|---|
| 400 | `VALIDATION_FAILED` | 数量为 0／负数／小数股，组合名超长 | EC-06,EC-08,EC-10 |
| 404 | `SECURITY_NOT_FOUND` | 股票代码不存在 | EC-05 |
| 404 | `PORTFOLIO_NOT_FOUND` | 组合不存在 | — |
| 404 | `HOLDING_NOT_FOUND` | 持仓不存在 | — |
| 404 | `ARTICLE_NOT_FOUND` | 新闻不存在 | — |
| 403 | `ADMIN_TOKEN_INVALID` | 管理端点缺少或传错 `X-Admin-Token` | — |
| 401 | `API_KEY_INVALID` | 对外只读 API 的 `X-API-Key` 无效 | — |
| 409 | `TASK_ALREADY_RUNNING` | 刷新／重算任务已在执行 | EC-20 |
| 503 | `UPSTREAM_UNAVAILABLE` | 外部数据源不可用**且无缓存可用** | EC-11 |

**重要：`503` 应当极少出现。** 按决策 2，外部数据源挂掉时读接口返回缓存数据 +
`stale: true`，而不是报错。只有系统从没成功拉取过数据时才会 503。

## 6.1 骨架恒定，`errors` 只是附加字段

**所有错误响应共用同一个骨架**（`type` / `title` / `status` / `detail` / `instance` / `code`），
校验失败只是在骨架上**追加**一个 `errors` 数组，不改变任何已有字段：

```json
{
  "type": "https://api.example.com/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request body has 2 invalid fields",
  "instance": "/api/v1/portfolios/1/holdings",
  "code": "VALIDATION_FAILED",
  "errors": [
    { "field": "quantity",  "message": "必须为正整数",  "rejectedValue": "-5" },
    { "field": "costBasis", "message": "不能为负数",    "rejectedValue": "-1.00" }
  ]
}
```

这样前端的全局错误处理器只需要一条路径：

```
读 code → 决定行为；若存在 errors → 额外做字段级高亮
```

**不需要为 400 写单独的解析分支。** `errors` 在非校验类错误里就是不存在（或空数组），
按可选字段处理即可。

后端实现要求：统一由一个 `@RestControllerAdvice` 产出全部错误响应，
不要在各 Controller 里手写错误体 —— 手写就一定会出现骨架不一致。

---

# 7. 前端开工清单

**前端现在就能开始，不用等后端。** 按这个顺序做：

1. [ ] 用 mock 层（MSW / json-server）把本文档的响应示例做成假数据
2. [ ] F1 组合总览：`summary` + 饼图（Chart.js）
3. [ ] F2 持仓管理：列表 + 新增表单 + 删除，表单校验按 §3 的规则；代码字段接 `/securities?q=` 做下拉补全（已实现，不用 mock）
4. [ ] F3 新闻流：列表 + 情感标签 + 分页 + symbol 筛选
5. [ ] **F4 联动视图**：`impact-view` 一个接口 + Chart.js annotation 竖线
6. [ ] F5 价值走势：折线图，处理空数据态
7. [ ] 全局：`asOf`/`stale` 的展示组件，一处写好各页复用

## 五个必须处理的空／异常态

这些不是边角情况，演示当天大概率会遇到：

| 场景 | 前端行为 | 对应 EC |
|---|---|---|
| 组合无持仓 | 空状态提示，不是空白页 | EC-01 |
| 新闻列表为空 | 「暂无新闻」 | EC-02 |
| 无影响评估记录 | 「暂无评估结果」+ 原因 | EC-03 |
| 系统无任何组合 | 引导创建第一个组合 | EC-04 |
| 价值走势无数据 | 「数据积累中」 | — |

## 三条容易写错的地方

**金额字段是字符串,不要 `Number()`。** 展示直接用,要算用 decimal 库。

**曲线归属看 `selectedSymbol`,不要靠 `impacts[0]` 猜。**
`impacts` 是数组且可能多条,切换股票要带 `?symbol=` 重新请求。

**`unrealizedPnLPct` 和 `directionAgreementRate` 可能是 `null`。**
分别对应成本价为 0 和样本不足,显示 `—` 而不是 `NaN`。

---

# 8. Swagger / OpenAPI

- 用 **springdoc-openapi** 从注解生成，**不手写 YAML**，避免与实现漂移
- `/swagger-ui` 暴露，内部接口与对外接口分成两个 `GroupedOpenApi`
- 仓库内附 `.http` 文件或 Postman collection，联调和演示都用它

后端接口实现后，**以 Swagger 生成的结果为准**，本文档转为需求侧参考。
若两者不一致，先确认是实现偏离契约还是契约需要更新，不要默认代码是对的。

---

# 9. 待定与后续

## 依赖架构决策的三处（前端按当前假设做即可）

| ⚠️ 位置 | 依赖 | 若决策改变，前端要做什么 |
|---|---|---|
| §1.3 `asOf`/`stale` | 决策 2 | 决策 2 被否才需要去掉展示组件，其余不动 |
| `/impacts/recompute` | 决策 3 | 端点可能消失，但它只是演示辅助，不影响任何页面 |
| ~~`engine` 字段~~ | ~~决策 5~~ | **已定案**：单一 Agent 方案，`engine` 已删除 |

**结论：即使剩下两个决策都被推翻，前端的页面结构和数据流都不用重写。**
这是把这几处显式标出来的目的。

## 本文档尚未定稿的部分

- 分页 `sort` 参数各接口的可选值尚未逐个枚举
- 对外只读 API 的限流阈值未定
- ~~`securities?q=` 的搜索行为（前缀匹配还是模糊匹配）未定~~ —— **已定案**：代码前缀 + 公司名包含，见 §4 的响应示例

剩下两项都不阻塞前端开工，等后端实现时补。

---

---

# 10. 与仓库 README 的其他冲突（需团队裁决，不是 API 问题）

同步 README 时发现三处**超出 API 范围**的冲突，比端点形状更需要尽快定：

| 冲突点 | README 写的 | 架构文档 v1.5 | 状态 |
|---|---|---|---|
| **情感分析实现** | finBERT + 独立 Python/Flask 微服务 | LLM API，Java 内直接调 | **仍冲突，需裁决** |
| `sentiment.news_id` UNIQUE | 建了 UNIQUE | 建 UNIQUE | ✅ 已一致 |
| Java 包名 | `com.fnpis` | `com.fnpis` | ✅ 已统一 |

**唯一剩下的冲突是情感分析的落地形态。** 团队已定「只做 Agent 方案」，
但 README 写的 finBERT 是**本地模型**（需求 D2 路线），不是 API 调用：

| | README 的 finBERT | 架构文档的 LLM API |
|---|---|---|
| 部署 | 多一个 Python/Flask 容器 | 无新增服务 |
| 体积 | 模型文件几百 MB | 无 |
| 成本 | 一次性下载，之后免费 | **每条新闻都计费** |
| 网络 | 本地推理，不依赖外网 | 依赖 LLM 服务可用性 |
| 集成测试 | 要多起一个依赖 | WireMock / Stub 即可 |

两条路都算"Agent 方案"的一种理解，但工程形态差别很大。
架构文档 v1.5 按 **LLM API** 写的（无新增服务、无模型文件）。
**如果团队实际想的是 finBERT 本地微服务，告诉我，架构文档要再改一轮** ——
`SentimentEngine` 接口不变，但 Docker Compose、项目结构、测试策略都要加一个服务。

我没有改动 README 里 finBERT 的技术栈描述，等这条定了再一起同步。

---

*API 契约 v0.2 草案结束 ｜ 后端实现后以 Swagger 为准*

