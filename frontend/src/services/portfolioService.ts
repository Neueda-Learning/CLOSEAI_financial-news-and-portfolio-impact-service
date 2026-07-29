import { portfolioSummaryMock, portfoliosMock } from '../mock/portfolioMock'
import type { Holding, Portfolio } from '../types/domain'

const storageKey = 'fnpis-portfolios-v1'
const activeStorageKey = 'fnpis-active-portfolio-v1'

const companyProfiles: Record<string, Pick<Holding, 'companyName' | 'currentPrice' | 'dayChangePct' | 'quoteSource'>> = {
  AAPL: { companyName: 'Apple Inc.', currentPrice: 189.12, dayChangePct: -0.52, quoteSource: 'LIVE' },
  NVDA: { companyName: 'NVIDIA Corporation', currentPrice: 125.6, dayChangePct: 3.46, quoteSource: 'LIVE' },
  MSFT: { companyName: 'Microsoft Corporation', currentPrice: 438.18, dayChangePct: 0.84, quoteSource: 'CACHE' },
  AMD: { companyName: 'Advanced Micro Devices', currentPrice: 159.9, dayChangePct: 2.8, quoteSource: 'LIVE' },
  TSLA: { companyName: 'Tesla Inc.', currentPrice: 251.4, dayChangePct: -1.18, quoteSource: 'CACHE' },
  GOOGL: { companyName: 'Alphabet Inc.', currentPrice: 184.72, dayChangePct: 0.66, quoteSource: 'LIVE' },
}

let portfolios = loadPortfolios()
let activePortfolioId = loadActivePortfolioId(portfolios)

function loadPortfolios() {
  try {
    const stored = window.localStorage.getItem(storageKey)
    if (!stored) return structuredClone(portfoliosMock)
    const parsed = JSON.parse(stored) as Portfolio[]
    return parsed.length > 0 ? parsed : structuredClone(portfoliosMock)
  } catch {
    return structuredClone(portfoliosMock)
  }
}

function loadActivePortfolioId(items: Portfolio[]) {
  const fallbackId = items[0]?.id ?? 1

  try {
    const stored = Number(window.localStorage.getItem(activeStorageKey))
    return items.some((portfolio) => portfolio.id === stored) ? stored : fallbackId
  } catch {
    return fallbackId
  }
}

function persist() {
  window.localStorage.setItem(storageKey, JSON.stringify(portfolios))
  window.localStorage.setItem(activeStorageKey, String(activePortfolioId))
}

function getActivePortfolio() {
  return portfolios.find((portfolio) => portfolio.id === activePortfolioId) ?? portfolios[0]
}

function calculateSummary(holdings: Holding[]) {
  const totalValue = holdings.reduce((sum, holding) => sum + holding.currentPrice * holding.shares, 0)
  const totalCost = holdings.reduce((sum, holding) => sum + holding.averageCost * holding.shares, 0)
  const todayChange = holdings.reduce((sum, holding) => {
    const previousPrice = holding.currentPrice / (1 + holding.dayChangePct / 100)
    return sum + (holding.currentPrice - previousPrice) * holding.shares
  }, 0)
  const todayChangePct = totalValue - todayChange === 0 ? 0 : (todayChange / (totalValue - todayChange)) * 100

  return {
    ...portfolioSummaryMock,
    totalValue,
    totalCost,
    totalPnL: totalValue - totalCost,
    totalPnLPct: totalCost === 0 ? 0 : ((totalValue - totalCost) / totalCost) * 100,
    todayChange,
    todayChangePct,
    allocation: holdings.map((holding) => {
      const value = holding.currentPrice * holding.shares
      return {
        ticker: holding.ticker,
        value,
        weight: totalValue === 0 ? 0 : (value / totalValue) * 100,
      }
    }),
  }
}

export const portfolioService = {
  async getSummary() {
    return calculateSummary(getActivePortfolio()?.holdings ?? [])
  },

  async getPortfolios() {
    return portfolios
  },

  async getActivePortfolioId() {
    return activePortfolioId
  },

  async setActivePortfolio(id: number) {
    if (portfolios.some((portfolio) => portfolio.id === id)) {
      activePortfolioId = id
      persist()
    }
  },

  async createPortfolio(name: string) {
    const created: Portfolio = {
      id: Date.now(),
      name: name.trim() || 'Untitled Portfolio',
      holdings: [],
      createdAt: new Date().toISOString(),
    }

    portfolios = [created, ...portfolios]
    activePortfolioId = created.id
    persist()
    return created
  },

  async deletePortfolio(id: number) {
    portfolios = portfolios.filter((portfolio) => portfolio.id !== id)

    if (portfolios.length === 0) {
      portfolios = structuredClone(portfoliosMock)
    }

    if (activePortfolioId === id || !portfolios.some((portfolio) => portfolio.id === activePortfolioId)) {
      activePortfolioId = portfolios[0].id
    }

    persist()
  },

  async getHoldings() {
    return getActivePortfolio()?.holdings ?? []
  },

  async addHolding(holding: Omit<Holding, 'id' | 'companyName' | 'currentPrice' | 'dayChangePct' | 'quoteUpdatedAt' | 'quoteSource'>) {
    const ticker = holding.ticker.toUpperCase()
    const profile = companyProfiles[ticker] ?? {
      companyName: `${ticker} Holding`,
      currentPrice: holding.averageCost,
      dayChangePct: 0,
      quoteSource: 'CACHE' as const,
    }
    const created: Holding = {
      id: Date.now(),
      ticker,
      companyName: profile.companyName,
      shares: holding.shares,
      averageCost: holding.averageCost,
      currentPrice: profile.currentPrice,
      dayChangePct: profile.dayChangePct,
      quoteUpdatedAt: new Date().toISOString(),
      quoteSource: profile.quoteSource,
    }

    portfolios = portfolios.map((portfolio) =>
      portfolio.id === activePortfolioId
        ? {
            ...portfolio,
            holdings: [created, ...portfolio.holdings],
          }
        : portfolio,
    )
    persist()
    return created
  },

  async updateHolding(id: number, holding: Pick<Holding, 'shares' | 'averageCost'>) {
    let updated: Holding | undefined

    portfolios = portfolios.map((portfolio) => {
      if (portfolio.id !== activePortfolioId) return portfolio

      return {
        ...portfolio,
        holdings: portfolio.holdings.map((item) => {
          if (item.id !== id) return item

          updated = {
            ...item,
            shares: holding.shares,
            averageCost: holding.averageCost,
          }
          return updated
        }),
      }
    })
    persist()
    return updated
  },

  async deleteHolding(id: number) {
    portfolios = portfolios.map((portfolio) =>
      portfolio.id === activePortfolioId
        ? {
            ...portfolio,
            holdings: portfolio.holdings.filter((holding) => holding.id !== id),
          }
        : portfolio,
    )
    persist()
  },
}
