import { apiFetch, asNumber, type PagedResponse } from './apiClient'
import type { ImpactEvent, Sentiment } from '../types/domain'

type NewsRow = { id: number; headline: string; source: string; url: string; publishedAt: string; symbols: string[]; sentiment: { label: Sentiment; score: number; confidence: number } | null; hasImpact: boolean }
type ImpactView = { article: { id: number; headline: string; source: string; url: string; publishedAt: string; sentiment: { label: Sentiment; score: number; confidence: number } | null }; impactedSymbols: string[]; selectedSymbol: string; impacts: Array<{ symbol: string; priceChangePct: number | null; valueImpact: string | null; direction: 'BULLISH' | 'BEARISH' | 'NEUTRAL'; alignment: 'CONFIRMED' | 'DIVERGENT' | 'INCONCLUSIVE' }>; priceSeries: { points: Array<{ t: string; price: string }> } | null }
type NewsDetail = { summary: string | null; image: string | null }

function mapView(view: ImpactView, detail?: NewsDetail): ImpactEvent {
  const primary = view.impacts[0]
  const sentiment = view.article.sentiment
  return {
    id: view.article.id, externalId: String(view.article.id), ticker: view.selectedSymbol, affectedTickers: view.impactedSymbols,
    headline: view.article.headline, source: view.article.source, url: view.article.url, publishedAt: view.article.publishedAt,
    sentiment: sentiment?.label ?? 'NEUTRAL', sentimentScore: sentiment?.score ?? 0, confidence: sentiment?.confidence ?? 0,
    impactDirection: primary?.direction ?? 'NEUTRAL', priceChange: primary?.priceChangePct ?? 0, portfolioImpact: asNumber(primary?.valueImpact),
    strength: 'Watch', alignment: primary?.alignment ?? 'INCONCLUSIVE', content: detail?.summary ?? '', summary: detail?.summary ?? null, image: detail?.image ?? null,
    priceSeries: view.priceSeries?.points.map((point) => ({ time: point.t, price: asNumber(point.price) })) ?? [],
  }
}

export const impactService = {
  async getImpactEvents(portfolioId: number, symbol?: string) {
    const query = new URLSearchParams({ page: '1', size: '20' })
    if (symbol) query.set('symbol', symbol)
    const news = await apiFetch<PagedResponse<NewsRow>>(`/news?${query}`)
    const results = await Promise.all(news.content.filter((row) => row.hasImpact).map(async (row) => {
      try { return mapView(await apiFetch<ImpactView>(`/news/${row.id}/impact-view?portfolioId=${portfolioId}`)) } catch { return null }
    }))
    return results.filter((event): event is ImpactEvent => event !== null)
  },
  async getImpactEventsForTickers(portfolioId: number, tickers: string[]) {
    const events = await this.getImpactEvents(portfolioId)
    const requested = new Set(tickers.map((ticker) => ticker.toUpperCase()))
    return events.filter((event) => event.affectedTickers.some((ticker) => requested.has(ticker)))
  },
  async getImpactEvent(id: number, portfolioId: number) {
    const [view, detail] = await Promise.all([
      apiFetch<ImpactView>(`/news/${id}/impact-view?portfolioId=${portfolioId}`),
      apiFetch<NewsDetail>(`/news/${id}`),
    ])
    return mapView(view, detail)
  },
  async getImpactSummary(portfolioId: number) {
    return apiFetch<{ weightedSentiment: number | null; newsCoverage: number | null; directionAgreementRate: number | null; sampleSize: number; counts: { confirmed: number; divergent: number; inconclusive: number }; asOf: string | null }>(`/portfolios/${portfolioId}/impact-summary`)
  },
  async refreshNews() { return apiFetch('/news/refresh', { method: 'POST' }) },
}
