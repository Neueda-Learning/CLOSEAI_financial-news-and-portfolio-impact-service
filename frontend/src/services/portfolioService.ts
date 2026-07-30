import { apiFetch, asNumber, type PagedResponse } from './apiClient'
import type { Holding, Portfolio } from '../types/domain'

type PortfolioDto = { id: number; name: string; totalMarketValue: string; holdingCount: number; createdAt: string; asOf: string | null; stale: boolean }
type HoldingDto = { id: number; symbol: string; companyName: string; quantity: string; costBasis: string; currentPrice: string | null; marketValue: string; totalCost: string; unrealizedPnL: string; unrealizedPnLPct: number | null; dayChangePct: number | null; weight: number; quoteAvailable: boolean }
type SummaryDto = { totalMarketValue: string; totalCost: string; unrealizedPnL: string; unrealizedPnLPct: number | null; dayChange: string | null; dayChangePct: number | null; allocations: Array<{ symbol: string; marketValue: string; weight: number }>; asOf: string | null; stale: boolean }

const activeStorageKey = 'fnpis-active-portfolio-v1'

function mapPortfolio(dto: PortfolioDto): Portfolio {
  return { id: dto.id, name: dto.name, holdings: [], createdAt: dto.createdAt, totalMarketValue: asNumber(dto.totalMarketValue), holdingCount: dto.holdingCount, stale: dto.stale, asOf: dto.asOf }
}

function mapHolding(dto: HoldingDto, asOf: string | null, stale: boolean): Holding {
  return {
    id: dto.id,
    ticker: dto.symbol,
    companyName: dto.companyName,
    shares: asNumber(dto.quantity),
    averageCost: asNumber(dto.costBasis),
    currentPrice: asNumber(dto.currentPrice),
    dayChangePct: dto.dayChangePct ?? 0,
    marketValue: asNumber(dto.marketValue),
    totalCost: asNumber(dto.totalCost),
    unrealizedPnL: asNumber(dto.unrealizedPnL),
    unrealizedPnLPct: dto.unrealizedPnLPct,
    weight: dto.weight * 100,
    quoteUpdatedAt: asOf,
    quoteSource: stale || !dto.quoteAvailable ? 'CACHE' : 'LIVE',
  }
}

export const portfolioService = {
  async getPortfolios() { return (await apiFetch<PortfolioDto[]>('/portfolios')).map(mapPortfolio) },
  async getActivePortfolioId(portfolios: Portfolio[]) {
    const stored = Number(window.localStorage.getItem(activeStorageKey))
    return portfolios.some((portfolio) => portfolio.id === stored) ? stored : portfolios[0]?.id ?? 0
  },
  async setActivePortfolio(id: number) { window.localStorage.setItem(activeStorageKey, String(id)) },
  async getSummary(id: number) {
    const dto = await apiFetch<SummaryDto>(`/portfolios/${id}/summary`)
    return {
      totalValue: asNumber(dto.totalMarketValue), totalCost: asNumber(dto.totalCost), totalPnL: asNumber(dto.unrealizedPnL), totalPnLPct: dto.unrealizedPnLPct ?? 0,
      todayChange: asNumber(dto.dayChange), todayChangePct: dto.dayChangePct ?? 0,
      allocation: dto.allocations.map((item) => ({ ticker: item.symbol, value: asNumber(item.marketValue), weight: item.weight * 100 })),
      asOf: dto.asOf, stale: dto.stale,
    }
  },
  async getHoldings(id: number, asOf: string | null = null, stale = true) {
    const result = await apiFetch<PagedResponse<HoldingDto>>(`/portfolios/${id}/holdings?size=100`)
    return result.content.map((holding) => mapHolding(holding, asOf ?? result.asOf, stale || result.stale))
  },
  async createPortfolio(name: string) { return mapPortfolio(await apiFetch<PortfolioDto>('/portfolios', { method: 'POST', body: JSON.stringify({ name: name.trim() }) })) },
  async deletePortfolio(id: number) { await apiFetch<void>(`/portfolios/${id}`, { method: 'DELETE' }) },
  async addHolding(portfolioId: number, holding: { ticker: string; shares: number; averageCost: number }) {
    await apiFetch(`/portfolios/${portfolioId}/holdings`, { method: 'POST', body: JSON.stringify({ symbol: holding.ticker, quantity: holding.shares, costBasis: holding.averageCost }) })
  },
  async updateHolding(id: number, holding: { shares: number; averageCost: number }) {
    await apiFetch(`/holdings/${id}`, { method: 'PATCH', body: JSON.stringify({ quantity: holding.shares, costBasis: holding.averageCost }) })
  },
  async deleteHolding(id: number) { await apiFetch<void>(`/holdings/${id}`, { method: 'DELETE' }) },
  async getValuationHistory(id: number, symbol?: string) {
    const url = `/portfolios/${id}/valuation-history` + (symbol ? `?symbol=${encodeURIComponent(symbol)}` : '')
    const result = await apiFetch<{ points: Array<{ date: string; totalValue: string }> }>(url)
    return result.points.map((point) => ({ date: point.date, totalValue: asNumber(point.totalValue) }))
  },
}
