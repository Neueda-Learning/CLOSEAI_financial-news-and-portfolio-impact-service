import { apiFetch, asNumber, type PagedResponse } from './apiClient'
import type { AnalysisStatus, ImpactEvent, Sentiment } from '../types/domain'

type Article = { id: number; headline: string; source: string; url: string; publishedAt: string; sentiment: { label: Sentiment; score: number; confidence: number } | null; analysisStatus?: 'PENDING' | 'FAILED'; sentimentStatus?: 'PENDING' | 'FAILED' }
type NewsRow = Article & { symbols: string[]; hasImpact: boolean }
type ImpactView = { article: Article; impactedSymbols: string[]; selectedSymbol: string; impacts: Array<{ symbol: string; priceChangePct: number | null; valueImpact: string | null; direction: Sentiment; alignment: 'CONFIRMED' | 'DIVERGENT' | 'INCONCLUSIVE' }>; priceSeries: { points: Array<{ t: string; price: string }> } | null }
type NewsDetail = { summary: string | null; image: string | null }

export type ImpactEventPage = {
  content: ImpactEvent[]
  page: number
  totalPages: number
  totalElements: number
}

export type NewsNavigationItem = Pick<NewsRow, 'id' | 'headline' | 'source'>

function mapView(view: ImpactView, detail?: NewsDetail): ImpactEvent {
  const primary = view.impacts[0]
  const sentiment = view.article.sentiment
  return {
    id: view.article.id, externalId: String(view.article.id), ticker: view.selectedSymbol, affectedTickers: view.impactedSymbols,
    headline: view.article.headline, source: view.article.source, url: view.article.url, publishedAt: view.article.publishedAt,
    sentiment: sentiment?.label ?? null, analysisStatus: analysisStatus(view.article), sentimentScore: sentiment?.score ?? 0, confidence: sentiment?.confidence ?? 0,
    impactDirection: primary?.direction ?? 'NEUTRAL', hasImpact: view.impacts.length > 0, priceChange: primary?.priceChangePct ?? 0, portfolioImpact: asNumber(primary?.valueImpact),
    strength: 'Watch', alignment: primary?.alignment ?? 'INCONCLUSIVE', content: detail?.summary ?? '', summary: detail?.summary ?? null, image: detail?.image ?? null,
    priceSeries: view.priceSeries?.points.map((point) => ({ time: point.t, price: asNumber(point.price) })) ?? [],
  }
}

function analysisStatus(article: Article): AnalysisStatus {
  if (article.sentiment) return null
  return article.analysisStatus === 'FAILED' || article.sentimentStatus === 'FAILED' ? 'FAILED' : 'PENDING'
}

function mapNewsRow(row: NewsRow): ImpactEvent {
  const sentiment = row.sentiment
  return {
    id: row.id, externalId: String(row.id), ticker: row.symbols[0] ?? '', affectedTickers: row.symbols,
    headline: row.headline, source: row.source, url: row.url, publishedAt: row.publishedAt,
    sentiment: sentiment?.label ?? null, analysisStatus: analysisStatus(row), sentimentScore: sentiment?.score ?? 0, confidence: sentiment?.confidence ?? 0,
    impactDirection: 'NEUTRAL', hasImpact: false, priceChange: 0, portfolioImpact: 0,
    strength: 'Watch', alignment: 'INCONCLUSIVE', content: '', summary: null, image: null, priceSeries: [],
  }
}

export const impactService = {
  /** Paginated news stream with optional filters — server-side filtering, no full-table scan. */
  async getImpactEvents(portfolioId: number, page = 1, opts?: { symbol?: string; analyzed?: boolean }): Promise<ImpactEventPage> {
    const query = new URLSearchParams({ page: String(page), size: '20' })
    if (opts?.symbol) query.set('symbol', opts.symbol)
    if (opts?.analyzed !== undefined) query.set('analyzed', String(opts.analyzed))
    const news = await apiFetch<PagedResponse<NewsRow>>(`/news?${query}`)
    const results = await Promise.all(news.content.map(async (row) => {
      if (!row.hasImpact) return mapNewsRow(row)
      try { return mapView(await apiFetch<ImpactView>(`/news/${row.id}/impact-view?portfolioId=${portfolioId}`)) } catch { return mapNewsRow(row) }
    }))
    return {
      content: results,
      page: news.page,
      totalPages: news.totalPages,
      totalElements: news.totalElements,
    }
  },
  async getImpactEventsForTickers(portfolioId: number, tickers: string[], page = 1): Promise<ImpactEventPage> {
    const result = await this.getImpactEvents(portfolioId, page)
    const requested = new Set(tickers.map((ticker) => ticker.toUpperCase()))
    return { ...result, content: result.content.filter((event) => event.affectedTickers.some((ticker) => requested.has(ticker))) }
  },
  async getImpactEvent(id: number, portfolioId: number) {
    const [view, detail] = await Promise.all([
      apiFetch<ImpactView>(`/news/${id}/impact-view?portfolioId=${portfolioId}`),
      apiFetch<NewsDetail>(`/news/${id}`),
    ])
    return mapView(view, detail)
  },
  async getNextNews(id: number): Promise<NewsNavigationItem | null> {
    let page = 1
    let totalPages = 1

    while (page <= totalPages) {
      const result = await apiFetch<PagedResponse<NewsRow>>(`/news?page=${page}&size=20`)
      totalPages = result.totalPages
      const index = result.content.findIndex((article) => article.id === id)
      if (index >= 0) {
        const next = result.content[index + 1]
        if (next) return { id: next.id, headline: next.headline, source: next.source }
        if (page < totalPages) {
          const nextPage = await apiFetch<PagedResponse<NewsRow>>(`/news?page=${page + 1}&size=20`)
          const first = nextPage.content[0]
          return first ? { id: first.id, headline: first.headline, source: first.source } : null
        }
        return null
      }
      page += 1
    }

    return null
  },
  async getPreviousNews(id: number): Promise<NewsNavigationItem | null> {
    let page = 1
    let totalPages = 1

    while (page <= totalPages) {
      const result = await apiFetch<PagedResponse<NewsRow>>(`/news?page=${page}&size=20`)
      totalPages = result.totalPages
      const index = result.content.findIndex((article) => article.id === id)
      if (index >= 0) {
        const previous = result.content[index - 1]
        if (previous) return { id: previous.id, headline: previous.headline, source: previous.source }
        if (page > 1) {
          const previousPage = await apiFetch<PagedResponse<NewsRow>>(`/news?page=${page - 1}&size=20`)
          const last = previousPage.content.at(-1)
          return last ? { id: last.id, headline: last.headline, source: last.source } : null
        }
        return null
      }
      page += 1
    }

    return null
  },
  async getImpactSummary(portfolioId: number) {
    return apiFetch<{ weightedSentiment: number | null; newsCoverage: number | null; directionAgreementRate: number | null; sampleSize: number; counts: { confirmed: number; divergent: number; inconclusive: number }; asOf: string | null }>(`/portfolios/${portfolioId}/impact-summary`)
  },
  async refreshNews() { return apiFetch<{ fetched: number; inserted: number; skippedDuplicates: number }>('/news/refresh', { method: 'POST' }) },
  async refreshSentiment() { return apiFetch<{ analysed: number; stored: number }>('/sentiment/refresh', { method: 'POST' }) },
}
