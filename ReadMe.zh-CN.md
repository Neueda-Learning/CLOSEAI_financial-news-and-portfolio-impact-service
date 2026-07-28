# 金融新闻与投资组合影响服务 (FNPIS)

> **项目 #15 · 第 7 组** — 聚合投资组合中公司的金融新闻，结合金融新闻 API 和股票价格 API，评估哪些持仓受到影响。用 LLM Agent 给每条标题打情感分，再回答一个更难的问题：价格是否真的按新闻暗示的方向动了。

[English Version](./ReadMe.md)

---

## 目录

1. [概述](#概述)
2. [设计文档](#设计文档)
3. [团队](#团队)
4. [技术栈](#技术栈)
5. [架构](#架构)
6. [项目结构](#项目结构)
7. [快速开始](#快速开始)
8. [REST API 文档](#rest-api-文档)
9. [数据库设计](#数据库设计)
10. [外部 API 集成](#外部-api-集成)
11. [情感分析](#情感分析)
12. [前端页面](#前端页面)
13. [定时任务](#定时任务)
14. [测试](#测试)
15. [CI/CD 与 Docker](#cicd-与-docker)
16. [Git 工作流](#git-工作流)
17. [项目管理](#项目管理)
18. [演示计划](#演示计划)

---

## 概述

### 问题

投资者持有股票组合，却难以将突发金融新闻与持仓的实际影响联系起来。一条关于苹果削减生产预测的头条新闻应当立即意味着潜在的价格下跌——但大多数投资者在几小时后才发现这个关联。

### 我们的方案

FNPIS 是一个全栈 Web 应用，实现以下功能：

1. **聚合** — 通过 Finnhub API 获取用户投资组合中每家公司的金融新闻
2. **分析** — 用 LLM Agent 对每条标题做情感判定（正面 / 负面 / 中性）
3. **评估** — 按持仓权重，把新闻情感与当日价格涨跌放在一起比对
4. **可视化** — 左右对比展示：左侧新闻、右侧价格图表

### 这个系统的输出为什么可信

系统把两个问题**分开回答，绝不揉成一个分数**：

| 问题 | 输出 | 来源 |
|------|------|------|
| 这条新闻*本该*让持仓怎么动？ | `Direction` — POSITIVE / NEGATIVE / NEUTRAL | 情感分析 |
| 价格*实际*同意了吗？ | `Alignment` — CONFIRMED / DIVERGENT / INCONCLUSIVE | 当日涨跌幅 vs 情感 |

`DIVERGENT` 不是 bug。利空出来市场没跌，这才是有信息量的情况；
把它藏起来的系统只是在重复你本来的假设。

`INCONCLUSIVE` 也是一种正经答案，覆盖两种不该被包装成结论的情形：
涨跌幅小于阈值，本质是噪声；或者拿不到前收盘价，涨跌幅根本算不出来。

### 核心功能（按优先级排序）

| 优先级 | 功能 | 描述 | 验收标准 |
|--------|------|------|----------|
| P0 | 浏览记录 | 查看持仓、新闻流、影响评估 | 持仓列表含代码、股数、当前价格；新闻流分页正常 |
| P1 | 查看指标 | 组合价值走势、持仓估值、新闻与价格联动视图 | 联动视图能画出价格曲线加新闻竖线；价值走势能处理空数据 |
| P2 | 添加项目 | 添加持仓，手动触发新闻刷新 | 输入代码 + 股数 → 持仓出现 → 一个轮询周期内拿到新闻 |
| P3 | 删除项目 | 移除持仓 | 移除后持仓和估值中都不再出现 |

### 集成要求

FNPIS 集成了两类不同的外部数据接口，加一个 LLM API：

| API | 用途 | 认证 |
|-----|------|------|
| [Finnhub](https://finnhub.io/) 公司新闻 | 按股票代码拉新闻（1 年历史） | 免费 Key — 独立账号 |
| [Finnhub](https://finnhub.io/) 报价 + K 线 | 最新报价与历史日线 | 免费 Key — **另一个**账号 |
| LLM API | 标题情感判定 | API Key |

新闻和行情故意用不同的 key，这样一条链路把额度用光不会拖死另一条。
**限流器按 key 分别配置，绝不共用** —— 共用的话 15 分钟一次的新闻轮询会把限流器打满，
报价刷新跟着被拒，那两个账号就白开了。

单一服务商依赖是真风险，而且多个 key 解决不了：Finnhub 自己挂掉时两条链路一起断。
真正兜住这个风险的是 `PriceProvider` 接口 —— 换一家数据源只需新增一个实现类，
业务逻辑和数据表都不用动。

---

## 设计文档

**技术决策以架构文档为准。** 本文档是给人看的入门材料；两者不一致时，以架构文档为准，
本文档就是那个需要修的 bug。

| 文档 | 覆盖内容 |
|------|----------|
| [`docs/项目15-①需求文档.md`](docs/项目15-①需求文档.md) | 需求 A–G、情感规则、影响评估公式与算例、边界条件 EC-01~EC-24 |
| [`docs/项目15-②架构设计.md`](docs/项目15-②架构设计.md) | 分层、关键决策、数据模型、项目结构 |
| [`docs/项目15-③API契约.md`](docs/项目15-③API契约.md) | 端点、分页、错误格式。后端实现后由 Swagger 取代 |

---

## 团队

**团队名称：** CLOSEAI

| 姓名 | 角色 | 职责 |
|------|------|------|
| Evan Li | 待定 | 待定 |
| David Hu | 待定 | 待定 |
| Venessa Feng | 待定 | 待定 |
| Ethan SUN | 待定 | 待定 |
| Timothy Xue | 待定 | 待定 |

> 角色分配（后端负责人 / 前端负责人 / 其他角色）由团队在第 1 周内确定。

后端分三块，这个分法决定了谁在等谁：

| 范围 | 负责人 | 说明 |
|------|--------|------|
| 组合 + 持仓 CRUD | 一人 | **同时负责 Flyway 脚本和所有 `@Entity`，其他人都不碰 schema** |
| 新闻聚合 + 行情数据 | 一人 | — |
| 情感分析 + 影响评估 | 一人 | 等实体建好才能开工 |

`repository/` 接口各自写，基于共享的实体。schema 单人负责是刻意的：
多人同时改迁移脚本，是让所有人的本地启动一起失败的最快途径。

**导师（GitHub 观察者）：** `helppo2`、`tuistmessiah`

### 团队协作方式

- **自组织** — 团队自行决定分工方式（如 2 人后端 + 1 人前端，或全员完成后端再开发前端，或按项目指南建议采用结对编程）
- **定期同步** — 安排每日站会，保持团队士气，互相解除阻塞
- **单一仓库** — 所有工作位于同一个 GitHub 仓库中（公开或与导师及所有团队成员共享）
- **及时上报阻塞** — 遇到困难时，在延迟扩大前主动向导师求助

---

## 技术栈

| 层次 | 技术 | 选型理由 |
|------|------|----------|
| **后端** | Java 17 + Spring Boot 3 | 培训技术栈 |
| **前端** | SPA，框架由前端同学自选 | 不影响后端、数据模型和 API 契约 |
| **图表** | Chart.js 4 + annotation 插件 | 联动视图要在折线图上画新闻时刻竖线，这是前端唯一的硬约束 |
| **数据库** | MySQL 8 + Flyway | 版本化 SQL 迁移；全库 utf8mb4 |
| **情感分析** | LLM Agent，单一引擎 | 见[情感分析](#情感分析) |
| **外部 API** | Finnhub，新闻与行情各用独立 key | 按用途隔离额度 |
| **HTTP 客户端** | RestClient (Spring 6.1+) | — |
| **韧性组件** | Resilience4j | 限流、重试、熔断 |
| **本地缓存** | Caffeine + Spring Cache | 包在对外调用外层 |
| **定时调度** | Spring `@Scheduled` | 新闻轮询、报价刷新、收盘快照、影响重算 |
| **API 文档** | springdoc-openapi | 从注解生成，不手写 YAML |
| **CI/CD** | GitHub Actions | 每次 PR：检查 → 编译 → 测试 → 构建 |
| **容器化** | Docker + Docker Compose | 一键本地启动；可移植部署 |
| **版本控制** | Git + GitHub | 功能分支、PR 审查 |
| **项目管理** | [Jira](https://therain2026.atlassian.net/jira/software/projects/FNPIS/boards/3) | 任务跟踪、Kanban 看板 |

**不设性能指标。** 端到端延迟由 Finnhub 的轮询周期主导，写一个响应时间目标是自欺欺人。
真正的质量标准是需求文档里的成功标准。

### 实现约定

以下几条只要有一个人不遵守就会出问题。

**金额和股数一律 `BigDecimal`，禁止 `double`/`float`。** 浮点运算会给出
`0.1 + 0.2 = 0.30000000000000004`，讲师现场手工验算就会对不上。
MySQL 侧用 `DECIMAL`。Checkstyle 里有规则拦命名像金额的 `double`，会直接让构建失败。

**JSON 里的金额字段序列化为字符串。** JavaScript 的 `Number` 是双精度浮点，
大额数值传过去会丢精度。前端只负责展示，不做金额运算。

**时间统一 UTC `Instant`，展示层再转时区。** Finnhub 返回 Unix 时间戳，
美股在美东时区交易，用户又在另一个时区看页面 —— 三个时区同时在场，
漏转一次就会静默落到错误的归因日。JDBC 连接串显式设了 `connectionTimeZone=UTC`。

**枚举存字符串**，用 `@Enumerated(EnumType.STRING)`。存序号的话，
以后往枚举中间插一个值，所有历史行的含义会静默错位。

---

## 架构

```
┌─────────────────────────────────────────────────┐
│  前端 SPA                                        │
│   组合总览 / 持仓管理 / 新闻流 / 联动视图          │
└──────────────────┬──────────────────────────────┘
                   │ REST + JSON
┌──────────────────▼──────────────────────────────┐
│  API 层                                          │
│   /api/v1/**     内部接口（前端用）               │
│   /public/v1/**  对外只读（讲师用，API Key）      │
│   /swagger-ui    接口文档                        │
└──────────────────┬──────────────────────────────┘
┌──────────────────▼──────────────────────────────┐
│  应用服务层 —— 业务逻辑全在这层                    │
│   PortfolioService    持仓 CRUD、估值             │
│   NewsService         聚合、去重、关联             │
│   SentimentService    情感分析（LLM Agent）        │
│   ImpactService       影响评估 ★核心              │
│   MarketDataService   报价与历史                  │
└────────┬──────────────────────────┬──────────────┘
         │                          │
┌────────▼───────────────┐  ┌───────▼─────────────┐
│  集成层（防腐层）        │  │  调度层             │
│   NewsProvider         │  │   新闻轮询          │
│   PriceProvider        │  │   报价刷新          │
│   SentimentEngine      │  │   收盘快照          │
│                        │  │   影响重算          │
│  实现：Finnhub / Mock  │  └───────┬─────────────┘
│  Caffeine 缓存         │          │
│  Resilience4j          │          │
└────────┬───────────────┘          │
┌────────▼──────────────────────────▼─────────────┐
│  持久层 —— MySQL + 版本化迁移脚本                 │
└─────────────────────────────────────────────────┘
```

**系统有两个入口：HTTP 和时钟。** 这就是为什么这里是五层，
而不是普通 CRUD 应用的三层。

| 层 | 能做什么 | 绝对不能做 |
|----|----------|------------|
| API 层 | 参数校验、DTO 转换、鉴权 | 写业务逻辑 |
| 应用服务层 | 全部业务规则、事务 | 直接调用第三方 HTTP |
| 集成层 | 调第三方、缓存、重试、格式转换 | 写业务规则 |
| 调度层 | 触发任务 | 实现业务逻辑（要调服务层） |
| 持久层 | 存取数据 | 计算 |

> 最容易被破坏的一条是**"服务层不许直连第三方"**。
> 为了赶进度在 Service 里内联一个 HTTP 调用，就同时丢掉了可换数据源和优雅降级两个性质。
> Code Review 重点盯这个。

### 关键决策

**三个 Provider 接口。** `NewsProvider`、`PriceProvider`、`SentimentEngine`，
服务层只依赖接口。Finnhub 的 DTO 不许出现在任何 `service/` 的方法签名上 ——
一旦供应商的字段名渗进业务代码，换数据源就变成改业务逻辑。
Review 时看 `integration/finnhub/` 的引用方向。

**读请求绝不内联调用 Provider。** 所有外部数据都由定时任务预先落库，
读接口只查数据库。响应带 `asOf`（数据采集时间）和 `stale`（上游当前不可用或被限流），
所以上游挂掉的结果是拿到旧数据，不是报错页。这两个字段前端必须展示。

**影响评估结果落库，不在读请求里现算。** 读路径直接返回表里的行。

**新闻按股票代码精确拉取，不做全局 feed。** 公司新闻端点让文章与代码的关联
天然精确，不需要做文本匹配。

**日线是影响计算的唯一价格来源。** 用"当日收盘 vs 前一日收盘"保证结果可复现，
盘中报价只用于展示。

---

## 项目结构

包按**层**划分，与架构文档一致。

```
FNPIS/
├── backend/
│   ├── src/main/java/com/fnpis/
│   │   ├── api/                  # @RestController + DTO
│   │   │   ├── internal/         #   /api/v1/**
│   │   │   └── pub/              #   /public/v1/**（对外只读）
│   │   ├── service/              # 业务逻辑（@Service）
│   │   ├── integration/          # 防腐层
│   │   │   ├── NewsProvider.java、PriceProvider.java、SentimentEngine.java
│   │   │   ├── finnhub/          #   Finnhub 实现、DTO、映射
│   │   │   ├── mock/             #   离线实现，测试与演示兜底用
│   │   │   └── sentiment/        #   Agent / Stub 引擎、输出校验
│   │   ├── scheduler/            # @Scheduled 任务
│   │   ├── domain/               # @Entity + 枚举
│   │   ├── repository/           # Spring Data JPA 接口
│   │   ├── common/               # 分页信封、Freshness、RFC 7807 错误类型
│   │   └── config/               # 缓存、Jackson、OpenAPI、Resilience4j
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/migration/         # Flyway 脚本
│   │   └── prompts/              # sentiment-prompt.txt（版本化）
│   ├── src/test/java/...
│   ├── checkstyle.xml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
├── docs/                            # 需求、架构、API 契约
├── docker-compose.yml               # mysql + backend（前端就绪后加入）
├── .env.example                     # 提交这个
├── .env                             # gitignore 这个
├── .github/
│   ├── workflows/ci.yml
│   └── pull_request_template.md
├── ReadMe.md                        # 英文版项目文档
└── ReadMe.zh-CN.md                  # 中文版项目文档（本文件）
```

### 当前进度

后端骨架已就位：构建、配置、共享响应类型、领域枚举、Docker、CI。

`api/`、`service/`、`integration/`、`scheduler/`、`repository/` **还没建** ——
这五个包里每一个的方法签名都要引用 `@Entity`，而实体由专人负责，
正在和 Flyway 脚本一起写。实体还不存在就先建这些包，
会让全队的仓库都编译不过。

`common/` 不属于原定结构，是额外加的：分页信封、`Freshness`、错误类型
三个开发者都要 import，放在 `api/` 下会让某一个人的包持有所有人依赖的类型。

---

## 快速开始

### 环境要求

- **Java 17+**（Maven 由 wrapper 提供，不用自己装）
- **Docker & Docker Compose**
- **两个 Finnhub API Key** — [注册](https://finnhub.io/register)，一个账号跑新闻，一个跑行情
- **一个 LLM API Key** 用于情感分析

### 环境变量

复制模板后填值。`.env` 已 gitignore，必须保持这样。

```bash
cp .env.example .env
```

`.env.example` 是变量清单的权威来源，由 CI 保证同步。大致结构：

| 变量 | 说明 |
|------|------|
| `DB_HOST`、`DB_PORT`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_ROOT_PASSWORD` | 数据库 |
| `FINNHUB_KEY_NEWS` | 只用于新闻轮询 |
| `FINNHUB_KEY_PRICE` | 只用于报价和日线 |
| `LLM_API_KEY`、`LLM_BASE_URL`、`LLM_MODEL` | 情感引擎 |
| `SERVER_PORT` | 应用 |
| `PROVIDER_NEWS`、`PROVIDER_PRICE`、`PROVIDER_SENTIMENT` | `finnhub`/`mock`、`finnhub`/`mock`、`agent`/`stub` |

**所有密钥都没有默认值。** 在 `application.yml` 里写默认值，
等于把 key 提交进仓库的风险敞口，所以应用会在启动时快速失败并点名缺哪个变量，
而不是跑几分钟后收到一堆 401。日志里的 key 一律脱敏。

> LLM key 要最当心：Finnhub key 泄露最多是额度被用光，LLM key 泄露是**直接产生费用**。

三个 `PROVIDER_*` 全部切到离线实现，整个系统零外部调用即可运行。
E2E 测试用的就是这套，也是演示日的兜底方案。

### 快速启动（Docker）

```bash
git clone https://github.com/Neueda-Learning/CLOSEAI_financial-news-and-portfolio-impact-service.git
cd CLOSEAI_financial-news-and-portfolio-impact-service

cp .env.example .env
# 编辑 .env 填入 API Key

docker compose up -d

# 后端 API 文档:  http://localhost:8080/swagger-ui.html
# 健康检查:      http://localhost:8080/actuator/health
```

### 手动启动（开发模式）

```bash
# 终端 1 — 只起数据库
docker compose up -d mysql

# 终端 2 — 后端
cd backend
./mvnw spring-boot:run               # :8080

# 终端 3 — 前端（就绪后）
cd frontend
npm install
npm run dev
```

### 推送前

```bash
cd backend
./mvnw checkstyle:check              # 风格门禁
./mvnw clean verify                  # 编译 + 测试
```

---

## REST API 文档

启动后可访问 `http://localhost:8080/swagger-ui.html` 查看完整的交互式文档。

> **加分项：** 将 API 暴露给导师，使其在最终演示时可以查询系统信息（如 `GET /api/portfolios` 验证持仓、`GET /api/impact?portfolioId=1` 检查影响事件）。Swagger UI 同时作为文档和实时查验工具。

完整端点清单、请求响应结构和错误目录见
[`docs/项目15-③API契约.md`](docs/项目15-③API契约.md)。这里不重复抄一遍 ——
抄一份就是多一份会过期的副本。

### Base URL

```
/api/v1/**        内部接口，前端使用
/public/v1/**     对外只读，需要 API Key
```

### 通用约定

**分页对客户端是 1 基的。** `size` 默认 20，上限 100；超出上限是截断到 100，不是报错。

**新鲜度。** 多数响应带 `asOf`（底层数据的采集时间）和 `stale`（上游当前不可用或被限流）。
前端必须把这两个都展示出来。

**金额字段是 JSON 字符串**，不是数字。

**错误遵循 RFC 7807**（`application/problem+json`）。客户端判断稳定的 `code` 字段，
绝不要判断给人看的 `detail`：

```json
{
  "type": "https://fnpis.local/errors/security-not-found",
  "title": "Security not found",
  "status": 404,
  "detail": "No security with symbol 'XYZQ'",
  "code": "SECURITY_NOT_FOUND",
  "instance": "/api/v1/securities/XYZQ"
}
```

参数校验失败会追加一个 `errors` 数组，骨架的其余部分保持不变。

### 最重要的那个接口

`GET /api/v1/news/{id}/impact-view?portfolioId=1` 一次返回左右对比视图需要的全部数据：
文章及其情感结果、每只股票的影响行（权重、涨跌幅、预期 vs 实际、影响金额、方向、一致性），
以及标注了新闻时刻的价格序列。前端不需要拼接任何东西。

`GET /api/v1/portfolios/{id}/valuation-history` 在新装的系统上会正常返回空的 `points`
数组 —— 快照是每个交易日累积一条。前端要显示"数据积累中"，
不能是空白图表，也不能报错。

`POST /api/v1/news/refresh` 是演示用的手动触发。已有刷新在跑时返回 **409**，
不是静默无反应 —— 演示时静默比报错更尴尬。
连点两次，第二次应该返回 `inserted: 0`，这就是去重的验收点。

---

## 数据库设计

schema **由专人负责**，用 `backend/src/main/resources/db/migration/` 下的版本化
Flyway 脚本定义。字段级细节见架构文档，这里只给全貌。

```
portfolio 1──n holding n──1 security
                                │
news_article n──n article_security_link
     │
     └─1─1 sentiment_score

security 1──n price_quote    （最新报价，每个代码一条）
security 1──n price_bar      （历史日线）

impact_assessment ──► news_article + security + portfolio
```

| 表 | 主键 | 关键约束 |
|----|------|----------|
| `portfolio` | `id` | — |
| `holding` | `id` | FK portfolio，**UNIQUE** (portfolio_id, symbol) |
| `security` | `symbol` | 业务天然主键，不另造代理键 |
| `news_article` | `id` | **UNIQUE (external_id)** — 去重完全依赖它 |
| `article_security_link` | (article_id, symbol) | 复合主键，天然防重 |
| `sentiment_score` | `id` | **UNIQUE (article_id)** — 单引擎，一篇一条 |
| `price_quote` | `symbol` | 只留最新一条，写入走 upsert |
| `price_bar` | (symbol, trade_date) | 复合主键 |
| `impact_assessment` | `id` | **UNIQUE** (article_id, symbol, portfolio_id, attribution_date) + 索引 (portfolio_id, attribution_date) |
| `portfolio_valuation_snapshot` | (portfolio_id, snapshot_date) | FK portfolio，复合主键 |

### 三个建模要点

**`news_article.external_id` 必须唯一。** 去重完全靠它，没有这个约束，
定时任务每跑一轮就重复插入同一批新闻。

**新闻与股票是多对多，必须有独立关联表。** 一条新闻可以影响多只股票（"芯片股集体大涨"），
一只股票有多条新闻。在 `news_article` 上加一个 `symbol` 字段是这里最容易犯的错，
事后要改就得动迁移脚本和所有相关查询。关联表还要存"关联是怎么建立的"，
裸的 `@ManyToMany` 中间表存不了这个字段。

**`sentiment_score.article_id` 唯一**，这个约束顺带保证一篇新闻不会被分析两次 ——
任务重跑不会产生额外的 LLM 调用。`model_version` 即使不做多引擎对比也要保留：
换模型或改 prompt 之后，它是唯一能判断"这条结论是哪个版本算的"的线索。

### 迁移规则

- **已提交的脚本永不修改。** Flyway 存了校验和，改动过的文件会让其他人的 checkout
  在启动时全部失败。要改就加新版本。
- V1–V6 已保留。后续变更从 V7 开始，并通知团队。
- `ddl-auto: validate` —— Hibernate 不建表也不改表，只校验实体和 Flyway 建出来的
  schema 是否一致。不一致就启动失败，这正是想要的效果。

---

## 外部 API 集成

### Finnhub

| 端点 | 用途 | 使用的 key |
|------|------|-----------|
| `/company-news?symbol=AAPL&from=…&to=…` | 按代码拉新闻 | `FINNHUB_KEY_NEWS` |
| `/quote?symbol=AAPL` | 最新报价，含前收盘 | `FINNHUB_KEY_PRICE` |
| `/stock/candle?symbol=AAPL&resolution=D&…` | 历史日线 | `FINNHUB_KEY_PRICE` |

用哪个 key 是集成层的内部细节。`FinnhubNewsProvider` 只读新闻 key，
`FinnhubPriceProvider` 只读行情 key，业务代码完全不知道有几个 key 存在。

**限流。** 每个 key 一个独立的 Resilience4j 实例。共用一个的话，
新闻轮询会把限流器打满并顺带拖死报价刷新，那两个账号就没有意义了。
新闻 key 被 429 时只降级新闻这条链路 —— 行情照常工作，页面上只有新闻区显示过期标记。

**降级。** 读请求由数据库服务，所以被限流或上游宕机的结果是拿到旧数据并标 `stale: true`，
不是报错。只有从来没成功拉取过的代码才会返回 `UPSTREAM_UNAVAILABLE`，
因为那种情况确实没有东西可返回。

**key 绝不进日志。** 请求 URL 记为 `token=***`。

### 已知风险

**免费版是否提供历史日线接口？** 影响引擎要从 `price_bar` 读"当日收盘 vs 前一日收盘"。
如果这个端点不可用，兜底方案是用每日收盘快照自建日线表 ——
而这**需要提前若干天开始跑**才有足够数据画图。
这是第一天优先级最高的调研项，而且多加 key 解决不了：额度问题和权限问题是两件事。

**限流是按 key 还是按 IP？** 如果按 IP，多个账号在同一台机器上不起作用，
多 key 方案会退化成单 key 加长轮询间隔。

**免费版条款是否允许一人多账号？** 多数服务商明确禁止，
最坏情况是演示前一天所有账号被封。无论如何都要保留 Mock Provider 可切换 ——
"希望 key 还活着"不是兜底方案。

---

## 情感分析

### 方案：LLM Agent，单一引擎

一个 LLM 驱动的 `SentimentEngine` 把每条标题判定为 POSITIVE / NEGATIVE / NEUTRAL，
附带分值和置信度。没有本地模型，也没有 Python 服务。

多引擎对比方案考虑过，放弃了：两个引擎意味着两倍的失败模式和两倍的演示讲解量，
换来一个没人要求的对比。

### 必须处理的情况

LLM 本质是一个会返回文本的网络调用，所以引擎把每个响应都当作不可信输入，
落库前先校验：

| 异常 | 处理 |
|------|------|
| 非法 label | 拒绝 |
| score 越界 | 拒绝 |
| 返回的不是 JSON | 拒绝 |
| score 与自己的 label 不自洽（`NEGATIVE` 配 `+0.8`） | 拒绝 |

每一条都有对应测试。temperature 设为 0，prompt 在 `resources/prompts/` 里版本化 ——
每行数据的 `model_version` 记录了是哪个 prompt 产出的，
所以改过 prompt 之后仍然能追溯旧结论的来源。

`StubSentimentEngine` 返回固定结果，不发网络请求。CI 和 E2E 测试用的是它，
这也是 CI 不需要真 key 的原因。

`sentiment_score.article_id` 的唯一约束保证一篇新闻不会被分析两次，重跑零成本。

---

## 前端页面

线框图见英文版 ReadMe 的 Frontend Pages 一节。

**凡是展示外部数据的页面都要显示 `asOf`，`stale` 为真时给出标记。**
这是产品功能，不是调试用的辅助信息 —— 上游挂掉的时候，靠它页面才还能读。

### 页面 1：投资组合仪表盘（P0）

持仓列表（代码、股数、价格、市值、涨跌幅）、当日情感统计（正面/负面/中性篇数）、
操作按钮（添加持仓、查看影响流）。

### 页面 2：新闻与影响流（核心演示页，P1）

影响评估卡片：情感标签与置信度、涨跌幅、持仓权重、金额影响，
以及方向（`POSITIVE`/`NEGATIVE`/`NEUTRAL`）和一致性（`CONFIRMED`/`DIVERGENT`/`INCONCLUSIVE`）。
这两个是分开的两个字段，不要合成一个分数。

### 页面 3：左右对比视图（演示亮点）

由 `/news/{id}/impact-view` 一次调用提供全部数据。
左侧是新闻文本加情感结论，右侧是价格折线图 ——
标出新闻时间点的那条竖线就是要用 Chart.js annotation 插件的原因。

### 页面 4：组合价值走势

数据来自 `/portfolios/{id}/valuation-history`。全新部署时 `points` 是空的，
因为快照是每个交易日累积一条 —— 这时候要渲染"正在积累数据"，
不是空图表，也不是报错。

---

## 定时任务

前端的任何操作都不会触发外部 API 调用。所有外部拉取都发生在这里，提前进行，落库备用。

| 任务 | 频率 | 描述 |
|------|------|------|
| 新闻轮询 | 每 15 分钟 | 遍历 watchlist 增量拉新闻，按 `external_id` 去重 |
| 情感分析 | 新闻入库后 | 只处理 `sentiment_score` 里没有记录的新闻 |
| 报价刷新 | 每 1 分钟，仅交易时段 | 更新 `price_quote` |
| 收盘快照 | 每日收盘后 | 写 `price_bar` + 组合价值快照 |
| 影响重算 | 收盘快照完成后 | 生成当日 `impact_assessment` |

```
新闻轮询 ──► 情感分析 ──┐
                        ├──► 影响重算
收盘快照 ───────────────┘
```

影响重算必须等两条支线都完成，否则跑在不全的数据上。

**收盘快照别省。** 它是组合价值走势图唯一的数据来源，而且事后补不回来 ——
`price_bar` 留了历史价格，但历史**持仓**没有留，用今天的持仓乘一个旧收盘价回答的是另一个问题。
每漏一晚就永久少一个数据点。

用 `fixedDelay` 而不是 `fixedRate`，任务就不可能和自己重叠。手动触发走的是 HTTP 线程，
仍然需要显式锁，拿不到锁返回 409。**跳过，不排队** —— 新闻轮询是幂等的，
跳一轮最多晚 15 分钟；排队会在上游恢复时一起打过去，反而触发限流。

---

## 测试

| 层次 | 工具 | 覆盖什么 |
|------|------|---------|
| 单元 | JUnit 5 + Mockito | Provider 全部 mock。**影响引擎必须有单元测试** —— 需求文档里那三个算例就是为了直接当用例抄进去的 |
| 集成 | `@SpringBootTest` + **Testcontainers MySQL** | 定时任务、去重、Flyway 脚本，跑在真实数据库上 |
| 集成层 | **WireMock** | 字段映射、限流重试、错误处理 |
| 情感校验 | JUnit 5 | LLM 返回的异常响应，见 [情感分析](#情感分析) |
| 前端 | Jest + React Testing Library | 组件渲染、mock 的 API 响应 |
| E2E | Playwright 或 Cypress | Provider 全部走 Mock，结果确定且不依赖网络 |

**用 Testcontainers 起真 MySQL，不用 H2。** H2 的兼容模式在 `DECIMAL` 精度、
日期函数、唯一索引长度限制上都和 MySQL 不一致 —— 一个在 H2 上过了的 Flyway 脚本
仍然可能在真 MySQL 上失败，那这个测试比没有更糟：它报告了一个自己没验证过的安全性。

需求文档列了 24 个边界情况（EC-01–EC-24），都是从真实 bug 场景写出来的，目标是一个一个测。
其中三个是除零陷阱，必须覆盖：成本基数为零、缺前收盘价、算权重时总市值为零。

### 端到端场景

```
1. 添加持仓        → 出现在组合里
2. 触发新闻刷新    → 新闻流填充；再点一次插入 0 条（去重）
3. 情感标签        → 出现在文章上
4. 触发影响重算    → 生成影响评估
5. 打开对比视图    → 图表渲染并画出新闻标记线
6. 删除持仓        → 验证清理完成
```

---

## CI/CD 与 Docker

### CI 流水线（`.github/workflows/ci.yml`）

Job 按条件激活 —— 目录还不存在的 job 是跳过，不是失败。

| Job | 何时激活 | 执行内容 |
|-----|---------|---------|
| `commitlint` | 总是 | Conventional Commits，检查 PR 里的每一个提交 |
| `backend-lint` | `backend/` 存在 | Checkstyle |
| `backend-type-check` | `backend/` 存在 | `mvn compile` |
| `backend-test` | `backend/` 存在 | JUnit 5 |
| `frontend-lint` | `frontend/` 存在 | ESLint |
| `frontend-type-check` | `frontend/tsconfig.json` 存在 | `tsc --noEmit` |
| `frontend-test` | `frontend/` 存在 | Jest |
| `build` | 总是 | 校验 compose 配置 + 打后端 jar |

`build` 会先用 `.env.example` 生成 `.env`：compose 文件用的是必填变量语法，
没有 `.env` 插值会直接失败。这也顺带把这个 job 变成了模板的同步检查 ——
加了一个必填变量却没写进 `.env.example`，这里就会红。

**CI 不需要真 key。** Finnhub 用 WireMock 打桩，情感分析用 `StubSentimentEngine`。
Testcontainers 在 GitHub 的 ubuntu runner 上直接可用，Docker 是预装的。

> **关键规则：** CI 必须同时在 PR 和 push 到 `dev`/`master` 时触发。Push 触发用于捕获两个 PR 单独通过但合并后破坏 `dev` 的情况。

### Docker

三个服务：`mysql`、`backend`、`frontend`（在有 Dockerfile 之前先注释掉）。
文件本身就是准确来源，见 [`docker-compose.yml`](docker-compose.yml)。其中四处是承重的：

**utf8mb4 在服务端显式指定。** 新闻标题里带 emoji 和各种符号，普通 `utf8` 存不下，
而结果是插入直接失败，不是降级存储。

**MySQL 的 healthcheck 不是可选项。** 后端一启动 Flyway 就连库，
而容器是在进程启动好几秒之后才开始接受连接。没有 `depends_on: condition: service_healthy`，
后端会在启动时直接挂掉。

**密钥用必填变量语法**（`${MYSQL_PASSWORD:?…}`），少给一个值会立刻失败并报出变量名，
而不是悄悄用空密码启动。

**后端镜像是多阶段的。** 先拷 `pom.xml` 解析依赖，再拷源码，
所以改一个 Java 文件不会把依赖重新下一遍。运行层只带 JRE 不带 Maven，
以非 root 用户运行，堆大小按容器的内存限制算而不是按宿主机。

---

## Git 工作流

### 分支策略

```
master          ← 发布分支（受保护 — 禁止直接推送）
  ├── hotfix/*   ← 紧急修复（源：master，目标：master）
  └── dev        ← 集成开发分支（受保护 — 禁止直接推送）
        ├── feature/*   ← 新功能（→ dev）
        ├── fix/*       ← Bug 修复（→ dev）
        ├── docs/*      ← 文档（→ dev）
        ├── refactor/*  ← 重构（→ dev）
        ├── chore/*     ← 工具 / CI / 依赖（→ dev）
        └── release/*   ← 发版候选（→ master）
```

### 分支命名规范

所有分支名使用 **kebab-case**（小写 + 连字符）。

| 前缀 | 用途 | 示例 |
|------|------|------|
| `feature/` | 新功能 | `feature/nlp-sentiment`、`feature/add-holding-form` |
| `fix/` | Bug 修复 | `fix/price-cache-timeout`、`fix/sentiment-score-range` |
| `hotfix/` | 紧急生产 Bug（从 `master` 分支） | `hotfix/crash-on-login`、`hotfix/api-key-expired` |
| `release/` | 发版候选（从 `dev` 分支，目标 `master`） | `release/v0.1.0`、`release/v1.0.0` |
| `docs/` | 纯文档 | `docs/swagger-descriptions`、`docs/setup-guide` |
| `refactor/` | 重构（不改变功能） | `refactor/impact-correlator`、`refactor/extract-common-charts` |
| `chore/` | 工具、CI、依赖 | `chore/update-docker-compose`、`chore/add-pre-commit-hooks` |

### 提交信息规范（Conventional Commits）

```
<type>: <简短描述>

feat: 添加 Finnhub 公司新闻端点及缓存
fix: 修复股票代码无价格数据时的空指针异常
docs: 补充影响关联算法文档
refactor: 提取价格归一化到共享工具类
test: 添加情感分数映射的边界测试
chore: 将 Docker Compose 的 MySQL 固定到 8.4
```

### 规则

#### 分支保护

| 规则 | `master` | `dev` |
|------|----------|-------|
| 直接推送 | ❌ 禁止 | ❌ 禁止 |
| 必须 Pull Request | ✅ | ✅ |
| 必须审查（≥1 人） | ✅ | ✅ |
| 必须 CI 通过 | ✅ | ✅ |
| 必须解决所有对话 | ✅ | ✅ |

#### 日常工作流

1. **开始** — 拉取最新 `dev`，从 `dev` 创建功能分支
2. **提交** — 使用 Conventional Commits；频繁提交，信息有意义
3. **保持同步** — 定期将 `dev` 合并到自己的功能分支，避免大量冲突
4. **推送并开 PR** — 推送分支，创建目标为 `dev` 的 PR，填写 PR 模板
5. **审查** — 至少一人审查并批准；CI 必须通过
6. **合并** — 使用 **Merge Commit**（不使用 squash 或 rebase）保留完整分支历史
7. **清理** — 合并后删除功能分支（GitHub 可自动完成）
8. **监控 dev CI** — PR 合并后检查 `dev` CI 是否仍然通过。若失败，立即停止新功能开发，创建 `fix/*` 分支

#### 合并冲突解决

1. **PR 作者负责**解决自己 PR 的冲突
2. 解决前，拉取最新 `dev` 并在本地合并
3. **若冲突不清楚**（如另一个队友改了同一逻辑）— 在团队频道联系对方再解决
4. GitHub "Resolve conflicts" 按钮可用于简单冲突（空格、import）；复杂冲突必须本地解决并重新审查
5. 存在未解决冲突的 PR **禁止合并**

#### Dev CI 失败处理

若 `dev` CI 在合并后失败（两个 PR 单独通过，合并后冲突）：

1. **停止** — 不要从损坏的 `dev` 上切分支
2. **团队负责人**（或最先发现者）创建 `fix/ci-dev-<问题>` 分支
3. 此修复优先于所有功能开发
4. 修复合并回 `dev` 后，恢复功能开发

#### 紧急修复流程（`master` 严重 Bug）

1. 从 `master` 切分支：`hotfix/<描述>`
2. 修复 + 提交（`fix: ...`）
3. 创建目标为 **`master`** 的 PR（不是 dev）
4. 至少一人审查 + CI 通过
5. Merge Commit 合入 `master`
6. 立即打 Tag：版本号递增 **patch** 位（`v0.1.0` → `v0.1.1`）
7. **立即**将 `master` 合并回 `dev`

#### 回滚错误合并

1. **首选**：从 `dev` 创建 `fix/*` 分支，修复 Bug，走正常 PR 流程
2. **如需立即回滚**：使用 `git revert`（绝不对共享分支使用 `git reset --hard`）
3. 原始 PR 作者排查根因并重新提交修正 PR

#### 发版流程（`dev` → `master`）

> **发版分支保护：** `release/*` 分支遵循与 `dev` 相同的规则 — 禁止直接推送、必须 PR、必须审查。

1. `dev` 稳定可发版后，从 `dev` 创建 `release/vX.Y.Z`
2. 创建 PR 从 `release/vX.Y.Z` → `master`
3. 全员审查
4. Merge Commit 合入 `master`
5. 在 `master` 上打 Tag
6. **发版 PR 作者**负责在打 Tag 后立即将 `master` 合并回 `dev`
7. 回合并成功后删除 `release/*` 分支

#### Tag 命名

| 阶段 | Tag | 时机 |
|------|-----|------|
| MVP | `v0.1.0` | 核心 CRUD 可用（第 2 周） |
| 迭代 | `v0.2.0`、`v0.3.0`… | 每个主要里程碑 |
| 最终 | `v1.0.0` | 最终演示后 |

#### 过期分支清理

- **草稿 PR** — 若 1 周内无活动，作者必须推送更新或关闭
- **废弃分支** — 过期分支（2 周以上无提交、无开放 PR）在每周同步时删除
- **每周检查** — 每周五团队审查所有开放分支和 PR；未活跃开发的分支重新分配或删除

### 仓库访问

- **单一仓库** — 一个 GitHub 仓库包含所有项目工作（按项目规范要求）
- **可见性** — 仓库必须公开，或与导师（`helppo2`、`tuistmessiah`）及所有团队成员共享
- **团队权限** — 确保每位成员在第 1 周结束前都能推送和创建 PR

### 仓库配置文件

| 文件 | 用途 |
|------|------|
| `.gitattributes` | 换行符统一（代码 LF，PowerShell 脚本 CRLF，图片 binary） |
| `.gitignore` | 排除系统文件、编辑器配置、`node_modules/`、`.env`、构建产物、密钥 |
| `.github/pull_request_template.md` | AI 自包含 PR 模板 — 包含 AI 代理填写完整 PR 所需的所有指令 |

---

## 项目管理

### 工具：Jira

10 人以下团队免费。使用 **Kanban** 项目（6 周时间线比 Scrum 更简单合适）。

**Issue 类型：**

| 类型 | 用途 |
|------|------|
| 长篇故事 (Epic) | 按模块分组（A–G、基础设施） |
| 故事 (Story) | 面向用户的功能（A1~A7, B1~B5, …, G1~G3） |
| 子任务 (Subtask) | 实现任务，挂在 Story 下 |
| Feature | 跨 Story 的技术能力 |
| 缺陷 (Bug) | 测试中发现的缺陷 |

**标签：** `p0`, `p1`, `p2`, `backend`, `frontend`, `core-logic`, `demo-hook`, `test`, `data`, `api`, `infra`

**流转状态：**

```
TODO  →  IN PROCESS  →  IN REVIEW  →  COMPLETED
  ↓
BLOCKED   （从 IN PROCESS 可拖入，独立列）
```

| 状态 | 含义 | 触发时机 |
|------|------|----------|
| **TODO** | 就绪，等待认领 | 创建 Issue 时默认 |
| **IN PROCESS** | 正在开发中 | 认领后自行拖拽 |
| **BLOCKED** | 被外部阻塞（等 API Key / 等队友 / 环境问题） | 随时 |
| **IN REVIEW** | PR 已开，等待队友审查 | 创建 PR 后拖拽 |
| **COMPLETED** | 已合入 `dev` | PR 合并后拖拽 |

**看板视图：**

| 视图 | 用途 | 使用者 |
|------|------|--------|
| **Kanban Board** | 日常拖拽，一目了然看进度 | 全员 |
| **By Assignee** | 查看每人当前工作负载 | 个人 |
| **By Epic** | 按周检查进度 — 本周所有项是否在线？ | 组长 / 站会 |
| **Backlog** | 每周 Grooming，排下一周 Story 优先级 | 全员 |

### 建议任务分解（最小 MVP）

| 周次 | 任务 | 交付物 | 依赖 |
|------|------|--------|------|
| **第 1 周** | 项目骨架、GitHub 仓库、数据库设计、Jira 配置 | 可运行的应用 + 数据库连接 | — |
| **第 2 周** | 投资组合 + 持仓 CRUD（后端 + 前端） | 可添加/查看/删除持仓 | 第 1 周 |
| **第 3 周** | Finnhub 集成：新闻获取 + 价格轮询 + 缓存 | 数据流入数据库 | 第 2 周 |
| **第 4 周** | LLM 情感引擎接入、影响计算引擎 | 生成 `impact_assessment` | 第 3 周 |
| **第 5 周** | 前端：影响流、左右对比视图、仪表盘图表 | 核心 UI 完成 | 第 4 周 |
| **第 6 周** | 打磨、测试、Swagger 文档、Docker、演示准备 | 可上线的 Demo | 第 5 周 |

### 持续实践

- **导师定期检查** — 导师会定期查看进度。准备一份问题清单，等导师来访时提问。
- **设计 + 构建并行** — 部分成员设计更完整的应用架构，同时其他成员构建小型可演示模块。
- **结对编程** — 在合适的场景尝试结对编程，对复杂逻辑（如影响关联算法）特别有效。

---

## 演示计划

> 15 分钟演示 + 5 分钟问答 — **讲一个故事！** 演示应有开头、中间和结尾。

### 演示规则（硬性要求）

| # | 规则 | 说明 |
|---|------|------|
| 1 | **每人都要发言** | 每位成员至少负责一个环节 — 不允许有人沉默 |
| 2 | **全程开启摄像头** | 整个演示过程中保持摄像头开启 |
| 3 | **向其他组提问** | 其他组演示后，你们需要提问 — 提前准备好有针对性的问题 |

### 演示流程

| 时间 | 发言人 | 内容 |
|------|--------|------|
| 0:00-1:00 | 组长 | 介绍团队成员；我们学到了什么；任务目标；开发周期（6 周） |
| 1:00-2:00 | 组长 | 项目方法 — 角色分工、工具、技术、团队名称 |
| 2:00-3:30 | 后端 | 高层架构（图示）、数据模型走查 — 解释设计决策 |
| 3:30-5:00 | 情感负责人 | 情感分析链路、为什么是单一 LLM 引擎、异常响应怎么挡掉 |
| 5:00-9:00 | **全员** | **现场演示 — "惊艳时刻"** |
| | | 5:00 — 展示组合仪表盘，一切正常 |
| | | 6:00 — 触发突发新闻："Apple 削减 iPhone 生产预测 1000 万台" |
| | | 6:30 — 情感分析即时显示 NEGATIVE（置信度 0.97） |
| | | 7:00 — 左右对比：新闻文本 vs 价格图表，-3.38% 跌幅 |
| | | 7:30 — 影响卡片：方向 NEGATIVE、一致性 CONFIRMED、金额影响 -$1,320.00 |
| | | 8:00 — 再看一条 `DIVERGENT` 的：新闻是负面的，市场没理它。这是有信息量的情况，不是 bug |
| | | 8:30 — 断开 Finnhub：页面照常显示，只是标上 `stale: true` |
| 9:00-11:00 | 团队 | 遇到的挑战 — 团队协作如何？技术难点？犯过什么错误？下次怎么做？ |
| 11:00-13:00 | 组长 | 如果有更多时间会做什么：多语言新闻、实时 WebSocket 告警、LLM 摘要 |
| 13:00-15:00 | **全员** | 感谢聆听 — 欢迎提问 |

### 演示准备清单

| # | 任务 | 负责人 |
|---|------|--------|
| 1 | 预加载 3-5 个股票代码的 50+ 条新闻到数据库 | 后端 |
| 2 | 预跑所有情感分析（标签即时显示，也不用在演示时花 LLM 额度） | 情感负责人 |
| 3 | 预算好 `impact_assessment`（读接口只查库，数据得先在） | 后端 |
| 4 | 端到端测试"触发新新闻"流程 | 全员 |
| 5 | 录制备份演示视频以防断网 | 前端 |
| 6 | 准备降级演示模式：若 Finnhub 宕机则切换至纯本地数据 | 后端 |
| 7 | 每位成员演练自己的环节，确保知道何时点击什么按钮 | 全员 |

---

## 备注

1. **用户管理：** 按项目规范，初始可假设为单用户。用户认证是可选的，仅在核心功能完成且时间充裕时添加。
2. **从小做起：** 第一个可用版本只需存储最简数据模型 — 仅含 `id`、`ticker` 和 `shares` 的投资组合。逐步增强。
3. **外部 API 韧性：** 每个项目都应展示外部 API 不可用时的降级行为。这里的做法是读接口只查库、响应带 `asOf` 和 `stale` — 上游宕机的结果是旧数据加一个标记，不是报错。演示时明确演一遍。
4. **质量优于数量：** 一个打磨好的 3 股票代码 Demo，带干净 UI 和能用的情感分析，胜过有 Bug 的 20 股票代码系统。
5. **保持敏捷：** 团队面临的最大问题是数据模型一开始就过于复杂。从 `Portfolio(id, name)` + `Holding(id, portfolioId, ticker, shares)` 开始。核心 CRUD 工作后再添加情感、价格和影响表。

---

## 许可证

本项目为 Final Project 培训项目的一部分。

---

> **"我们不只给你看新闻。我们让你看到新闻对你的钱意味着什么。"**
