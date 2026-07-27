# 金融新闻与投资组合影响服务 (FNPIS)

> **项目 #15** — 聚合投资组合中公司的金融新闻，结合金融新闻 API 和股票价格 API，评估哪些持仓受到影响。使用 NLP 情感分析将新闻与日内价格走势（5 分钟轮询）进行关联。

[English Version](./ReadMe.md)

---

## 目录

1. [概述](#概述)
2. [团队](#团队)
3. [技术栈](#技术栈)
4. [项目结构](#项目结构)
5. [快速开始](#快速开始)
6. [REST API 文档](#rest-api-文档)
7. [数据库设计](#数据库设计)
8. [外部 API 集成](#外部-api-集成)
9. [NLP 情感分析](#nlp-情感分析)
10. [前端页面](#前端页面)
11. [定时任务](#定时任务)
12. [测试](#测试)
13. [CI/CD 与 Docker](#cicd-与-docker)
14. [Git 工作流](#git-工作流)
15. [项目管理](#项目管理)
16. [演示计划](#演示计划)

---

## 概述

### 问题

投资者持有股票组合，却难以将突发金融新闻与持仓的实际影响联系起来。一条关于苹果削减生产预测的头条新闻应当立即意味着潜在的价格下跌——但大多数投资者在几小时后才发现这个关联。

### 我们的方案

FNPIS 是一个全栈 Web 应用，实现以下功能：

1. **聚合** — 通过 Finnhub API 获取用户投资组合中每家公司的金融新闻
2. **分析** — 使用 NLP 对每条新闻进行情感分析（正面 / 负面 / 中性）
3. **关联** — 将新闻情感与日内股票价格走势（5 分钟轮询）进行关联
4. **可视化** — 左右对比展示：左侧新闻、右侧价格图表

### 核心功能（按优先级排序）

| 优先级 | 功能 | 描述 | 验收标准 |
|--------|------|------|----------|
| P0 | 浏览记录 | 查看投资组合持仓、新闻流和影响事件 | 用户能看到所有持仓列表（含代码、股数、当前价格）；新闻流 ≤3 秒加载 |
| P1 | 查看指标 | 图形化仪表盘：情感趋势、价格影响关联、配置图表 | 仪表盘渲染 ≥2 种图表类型，使用近 30 天数据 |
| P2 | 添加项目 | 向投资组合添加持仓，触发新闻获取 | 用户输入代码 + 股数 → 持仓出现在组合中 → 15 分钟内获取新闻 |
| P3 | 删除项目 | 移除持仓、清除影响事件、清理历史数据 | 移除的持仓从仪表盘消失；关联的影响事件被软删除 |

### 集成要求

FNPIS 集成了 **两个** 外部 API（超过最低要求的一个）：

| API | 用途 | 认证方式 |
|-----|------|----------|
| [Finnhub](https://finnhub.io/) | 金融新闻（1 年历史）+ 股票价格（日内 & 历史） | 免费 API Key |
| [Alpha Vantage](https://www.alphavantage.co/) | 补充股票价格（备用） | 免费 API Key |

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

> 角色分配（后端负责人 / NLP 负责人 / 前端负责人 / 其他角色）由团队在第 1 周内确定。

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
| **前端** | React + Chart.js / D3.js | 单页应用，丰富的交互式图表 |
| **数据库** | MySQL 8 | 持久化存储持仓、新闻、价格、影响事件 |
| **NLP** | finBERT (ProsusAI) 通过 Python 微服务，或 LLM API 备用 | 金融领域情感分析；优先本地模型以保证可靠性 |
| **外部 API** | Finnhub（主）、Alpha Vantage（备用） | 新闻 + 股票价格 |
| **定时调度** | Spring `@Scheduled` | 定期新闻获取、价格轮询、影响关联 |
| **API 文档** | Swagger / OpenAPI 3.0 | 从注解自动生成 |
| **CI/CD** | GitHub Actions | 每次 PR 构建 → 测试 → 检查 |
| **容器化** | Docker + Docker Compose | 一键本地启动；可移植部署 |
| **版本控制** | Git + GitHub | 功能分支、PR 审查 |
| **项目管理** | Jira | 任务跟踪、迭代规划 |

---

## 项目结构

```
FNPIS/
├── backend/
│   ├── src/
│   │   ├── main/java/com/fnpis/
│   │   │   ├── controller/          # REST API 端点
│   │   │   │   ├── PortfolioController.java
│   │   │   │   ├── HoldingController.java
│   │   │   │   ├── NewsController.java
│   │   │   │   ├── ImpactController.java
│   │   │   │   └── DashboardController.java
│   │   │   ├── service/             # 业务逻辑
│   │   │   │   ├── PortfolioService.java
│   │   │   │   ├── NewsFetchService.java
│   │   │   │   ├── SentimentService.java
│   │   │   │   ├── PriceService.java
│   │   │   │   └── ImpactCorrelatorService.java
│   │   │   ├── model/               # JPA 实体
│   │   │   │   ├── User.java
│   │   │   │   ├── Portfolio.java
│   │   │   │   ├── Holding.java
│   │   │   │   ├── NewsArticle.java
│   │   │   │   ├── SentimentScore.java
│   │   │   │   ├── StockPrice.java
│   │   │   │   └── ImpactEvent.java
│   │   │   ├── repository/          # 数据访问层
│   │   │   ├── config/              # 应用配置、API Key、Swagger
│   │   │   └── scheduler/           # 定时任务定义
│   │   └── resources/
│   │       └── application.yml      # 数据库配置、API Key（环境变量）
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── PortfolioDashboard/
│   │   │   ├── NewsFeed/
│   │   │   ├── ImpactViewer/        # 左右对比：新闻 + 价格图表
│   │   │   ├── HoldingManager/
│   │   │   └── common/              # 图表、卡片、导航
│   │   ├── pages/
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── NewsImpactPage.jsx
│   │   │   └── PortfolioPage.jsx
│   │   ├── services/                # API 客户端调用
│   │   └── App.jsx
│   ├── Dockerfile
│   └── package.json
├── nlp-service/                     # Python 微服务（使用 finBERT 时）
│   ├── app.py                       # Flask/FastAPI 情感分析端点
│   ├── model.py                     # finBERT 模型加载 + 推理
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml               # 编排 backend + frontend + DB + nlp
├── .github/
│   ├── workflows/
│   │   └── ci.yml                   # 构建、检查、测试流水线
│   └── pull_request_template.md      # PR 模板（AI 自包含）
├── .gitattributes                   # 跨平台换行符统一
├── .gitignore                       # 排除敏感文件和构建产物
├── ReadMe.md                        # 英文版项目文档
└── ReadMe.zh-CN.md                  # 中文版项目文档（本文件）
```

---

## 快速开始

### 环境要求

- **Java 17+** + Maven
- **Python 3.10+**（用于 NLP 微服务）
- **Docker & Docker Compose**
- **MySQL 8+**（或使用 Docker 化的数据库）
- **Finnhub API Key** — [获取免费 Key](https://finnhub.io/register)

### 环境变量

在项目根目录创建 `.env` 文件（切勿提交此文件）：

```env
# 数据库
DB_HOST=localhost
DB_PORT=3306
MYSQL_DATABASE=fnpis
MYSQL_USER=fnpis_user
MYSQL_PASSWORD=your_db_password
MYSQL_ROOT_PASSWORD=your_root_password

# 外部 API
FINNHUB_API_KEY=your_finnhub_key
ALPHA_VANTAGE_API_KEY=your_alphavantage_key

# NLP 服务
NLP_SERVICE_URL=http://localhost:5001

# 应用
SERVER_PORT=8080
```

### 快速启动（Docker）

```bash
# 1. 克隆仓库
git clone https://github.com/therain2020/financial-news-and-portfolio-impact-service.git
cd FNPIS

# 2. 设置环境
cp .env.example .env
# 编辑 .env 填入 API Key

# 3. 一键启动所有服务
docker-compose up -d

# 4. 验证
# 后端 API 文档:  http://localhost:8080/swagger-ui.html
# 前端页面:      http://localhost:3000
# NLP 服务:      http://localhost:5001/health
```

### 手动启动（开发模式）

```bash
# 终端 1 — 数据库
docker-compose up -d db

# 终端 2 — NLP 服务
cd nlp-service
pip install -r requirements.txt
python app.py                        # 运行在 :5001

# 终端 3 — 后端
cd backend
./mvnw spring-boot:run               # 运行在 :8080

# 终端 4 — 前端
cd frontend
npm install
npm run dev                          # 运行在 :3000
```

---

## REST API 文档

启动后可访问 `http://localhost:8080/swagger-ui.html` 查看完整的交互式文档。

> **加分项：** 将 API 暴露给导师，使其在最终演示时可以查询系统信息（如 `GET /api/portfolios` 验证持仓、`GET /api/impact?portfolioId=1` 检查影响事件）。Swagger UI 同时作为文档和实时查验工具。

### Base URL

```
http://localhost:8080/api
```

### 端点

#### 投资组合

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/portfolios` | 列出用户的投资组合 |
| `POST` | `/portfolios` | 创建投资组合 |
| `GET` | `/portfolios/{id}` | 获取组合详情（含持仓、总价值） |
| `PUT` | `/portfolios/{id}` | 更新组合名称 |
| `DELETE` | `/portfolios/{id}` | 删除组合 |

#### 持仓

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/portfolios/{id}/holdings` | 列出组合中的持仓 |
| `POST` | `/portfolios/{id}/holdings` | 添加持仓 `{ticker, shares, avgCost}` |
| `PUT` | `/holdings/{id}` | 更新持仓（股数/成本） |
| `DELETE` | `/holdings/{id}` | 删除持仓 |

#### 新闻

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/news?ticker=AAPL&days=7` | 获取某股票代码的新闻（日期范围） |
| `GET` | `/news/portfolio/{portfolioId}` | 获取组合中所有持仓的新闻 |
| `GET` | `/news/{id}/sentiment` | 获取单篇文章的情感分析结果 |

#### 股票价格

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/prices?tickers=AAPL,TSLA` | 批量获取当前价格 |
| `GET` | `/prices/{ticker}/history?range=1m` | 获取历史价格（用于图表） |

#### 影响分析（核心）

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/impact?portfolioId=1&days=30` | 获取组合的所有影响事件 |
| `GET` | `/impact/{id}` | 单个影响事件详情（新闻 + 图表数据） |
| `GET` | `/impact/{id}/comparison` | 左右对比：新闻文本 vs 价格图表数据 |
| `POST` | `/impact/calculate?portfolioId=1` | 手动触发影响关联计算 |

#### 仪表盘

| Method | Path | 描述 |
|--------|------|------|
| `GET` | `/dashboard/summary?portfolioId=1` | 组合摘要 + 当日情感概览 |
| `GET` | `/dashboard/sentiment-trend?portfolioId=1&days=30` | 30 天情感趋势数据 |
| `GET` | `/dashboard/top-impact?portfolioId=1&limit=5` | 前 5 条新闻影响事件 |

---

## 数据库设计

完整 SQL 建表语句请参考英文版 ReadMe §7。

### 实体关系图

```
users 1--* portfolios 1--* holdings
                                  |
news_articles 1--1 sentiment_scores
       |                        |
       +-------- impact_events -+
                      |
              stock_prices（通过 ticker + timestamp 引用）
```

---

## 外部 API 集成

### Finnhub（主）

| 端点 | 用途 | 频率限制 |
|------|------|----------|
| `/api/v1/company-news?symbol=AAPL&from=...&to=...` | 公司新闻（1 年历史） | 60 次/分钟（免费） |
| `/api/v1/quote?symbol=AAPL` | 实时报价 | 60 次/分钟 |
| `/api/v1/stock/candle?symbol=AAPL&resolution=D&from=...&to=...` | 历史 K 线 | 60 次/分钟 |

**频率限制策略：** 所有响应缓存至 `stock_prices` 和 `news_articles` 表。绝不从前端直接调用 API。定时任务获取数据并填充本地缓存。

**降级策略：** 若 Finnhub 返回 429（频率限制）或 5xx，系统从本地缓存读取。若缓存过期（价格 >15 分钟、新闻 >1 小时），UI 显示"数据可能延迟"横幅。

### Alpha Vantage（备用）

| 端点 | 用途 |
|------|------|
| `GLOBAL_QUOTE&symbol=AAPL` | 实时报价备用 |
| `TIME_SERIES_DAILY&symbol=AAPL` | 历史数据备用 |

---

## NLP 情感分析

完整实现代码请参考英文版 ReadMe §9。

### 方案：finBERT（本地开源）

[finBERT](https://github.com/ProsusAI/finBERT) 是基于金融文本（SEC 文件、财报、分析师笔记）微调的 BERT 模型，在金融领域文本上优于通用情感模型。

**选择本地模型而非 LLM API 的理由：**

1. 零 API 成本，不受规模限制
2. 无频率限制 — 瞬间处理数百篇文章
3. 离线可用 — 断网也不影响演示
4. 金融领域准确性（finBERT 能区分 "beat estimates" 超预期 和 "missed earnings" 未达预期）

### 备用：LLM API（finBERT 不可用时）

仅当 NLP 微服务不可达时，后端通过 LLM API（DeepSeek / OpenAI / Claude）进行情感分析。

---

## 前端页面

完整 ASCII 线框图请参考英文版 ReadMe §10。

### 页面 1：投资组合仪表盘（P0）

展示持仓列表（代码、股数、价格、市值、涨跌幅）、当日情感统计（正面/负面/中性文章数）、操作按钮（添加持仓、查看影响流）。

### 页面 2：新闻与影响流（核心演示页，P1）

展示热门影响事件卡片（含情感标签、价格变化百分比、关联强度）、30 天情感趋势图。

### 页面 3：左右对比视图（演示亮点）

左侧：新闻文本 + 情感分析结果（标签、置信度、来源、发布时间）
右侧：价格折线图，标注新闻发布时间点，显示新闻前后价格变化

---

## 定时任务

| 任务 | 频率 | 描述 |
|------|------|------|
| `NewsFetcher` | 每 15 分钟 | 从 Finnhub 获取所有组合中所有股票代码的最新新闻 |
| `PricePoller` | 每 5 分钟 | 获取所有追踪股票代码的实时报价，缓存至 `stock_prices` |
| `SentimentAnalyzer` | NewsFetcher 之后 | 对所有未分析的文章运行 NLP 情感分析 |
| `ImpactCorrelator` | 每小时 | 对新文章：找到前后最近价格、计算影响、写入 `impact_events` |
| `CacheCleaner` | 每日 03:00 | 清理超过 90 天的价格数据、超过 1 年的新闻 |

---

## 测试

### 单元测试

```
后端（JUnit 5 + Mockito）:
  - Service 层：PortfolioService、SentimentService、ImpactCorrelatorService
  - 影响关联算法：边界情况（无价格数据、单边新闻）
  - 数据验证：股票代码格式、股数、百分比范围

NLP 服务（pytest）:
  - finBERT 推理：已知正面/负面/中性标题
  - 分数映射：置信度阈值 → 分数范围
  - 批处理：N 条输入 → N 条正确输出

前端（Jest + React Testing Library）:
  - 组件渲染：仪表盘卡片、影响事件行
  - API Mock 响应
```

### 端到端测试（Cypress / Playwright）

1. 添加持仓 → 验证显示在组合中
2. 触发新闻获取 → 验证新闻流填充
3. 验证新闻文章显示情感标签
4. 触发影响计算 → 验证影响事件生成
5. 打开左右对比视图 → 验证图表渲染并标注新闻
6. 删除持仓 → 验证清理完成

---

## CI/CD 与 Docker

### CI 流水线（`.github/workflows/ci.yml`）

| Job | 执行内容 | 触发条件 |
|-----|---------|----------|
| `lint` | Checkstyle (Java) / ESLint (前端) | PR 到 `dev`/`master`；push 到 `dev`/`master` |
| `type-check` | `javac` 编译检查 | PR 到 `dev`/`master` |
| `unit-test` | 后端：JUnit 5；前端：Jest + React Testing Library；NLP：pytest | PR 到 `dev`/`master` |
| `build` | 验证项目可编译/打包无错误 | PR 到 `dev`/`master` |
| `commitlint` | 拒绝不符合 Conventional Commits 的提交信息 | PR 到 `dev`/`master` |

> **关键规则：** CI 必须同时在 PR 和 push 到 `dev`/`master` 时触发。Push 触发用于捕获两个 PR 单独通过但合并后破坏 `dev` 的情况。

### Docker Compose

完整 `docker-compose.yml` 请参考英文版 ReadMe §13。

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
chore: 升级 Docker Compose 至 PostgreSQL 16
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

**看板列：** Backlog → To Do → In Progress → Review → Done

**Issue 类型：**

| 类型 | 用途 |
|------|------|
| Epic | 每周里程碑（第 1 周 ~ 第 6 周） |
| Story | 面向用户的功能（P0-P3 项） |
| Task | 技术工作项（如"搭建 Finnhub API 客户端"） |
| Bug | 测试中发现的缺陷 |

**标签：** `backend`、`frontend`、`nlp`、`devops`、`docs`

### 建议任务分解（最小 MVP）

| 周次 | 任务 | 交付物 | 依赖 |
|------|------|--------|------|
| **第 1 周** | 项目骨架、GitHub 仓库、数据库设计、Jira 配置 | 可运行的应用 + 数据库连接 | — |
| **第 2 周** | 投资组合 + 持仓 CRUD（后端 + 前端） | 可添加/查看/删除持仓 | 第 1 周 |
| **第 3 周** | Finnhub 集成：新闻获取 + 价格轮询 + 缓存 | 数据流入数据库 | 第 2 周 |
| **第 4 周** | NLP 情感服务上线、影响关联算法 | 影响事件生成 | 第 3 周 |
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
| 3:30-5:00 | NLP 负责人 | 情感分析流水线、finBERT、为什么选择本地模型而非 LLM API |
| 5:00-9:00 | **全员** | **现场演示 — "惊艳时刻"** |
| | | 5:00 — 展示组合仪表盘，一切正常 |
| | | 6:00 — 触发突发新闻："Apple 削减 iPhone 生产预测 1000 万台" |
| | | 6:30 — 情感分析即时显示 NEGATIVE（97%） |
| | | 7:00 — 左右对比：新闻文本 vs 价格图表，-3.38% 跌幅 |
| | | 7:30 — 影响卡片："STRONG 负相关" |
| | | 8:00 — 展示影响流中的多条影响事件 |
| 9:00-11:00 | 团队 | 遇到的挑战 — 团队协作如何？技术难点？犯过什么错误？下次怎么做？ |
| 11:00-13:00 | 组长 | 如果有更多时间会做什么：多语言新闻、实时 WebSocket 告警、LLM 摘要 |
| 13:00-15:00 | **全员** | 感谢聆听 — 欢迎提问 |

### 演示准备清单

| # | 任务 | 负责人 |
|---|------|--------|
| 1 | 预加载 3-5 个股票代码的 50+ 条新闻到数据库 | 后端 |
| 2 | 预执行所有情感分析（使标签即时显示） | NLP 负责人 |
| 3 | 预计算影响事件（使关联数据就绪） | 后端 |
| 4 | 端到端测试"触发新新闻"流程 | 全员 |
| 5 | 录制备份演示视频以防断网 | 前端 |
| 6 | 准备降级演示模式：若 Finnhub 宕机则切换至纯本地数据 | 后端 |
| 7 | 每位成员演练自己的环节，确保知道何时点击什么按钮 | 全员 |

---

## 备注

1. **用户管理：** 按项目规范，初始可假设为单用户。用户认证是可选的，仅在核心功能完成且时间充裕时添加。
2. **从小做起：** 第一个可用版本只需存储最简数据模型 — 仅含 `id`、`ticker` 和 `shares` 的投资组合。逐步增强。
3. **外部 API 韧性：** 每个项目都应展示外部 API 不可用时的降级行为。`stock_prices` 和 `news_articles` 表中的缓存层即为此目的 — 演示中应明确展示这一韧性。
4. **质量优于数量：** 一个打磨好的 3 股票代码 Demo，带干净 UI 和正常工作的 NLP，胜过有 Bug 的 20 股票代码系统。
5. **保持敏捷：** 团队面临的最大问题是数据模型一开始就过于复杂。从 `Portfolio(id, name)` + `Holding(id, portfolioId, ticker, shares)` 开始。核心 CRUD 工作后再添加情感、价格和影响表。

---

## 许可证

本项目为 Final Project 培训项目的一部分。

---

> **"我们不只给你看新闻。我们让你看到新闻对你的钱意味着什么。"**
