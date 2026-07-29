import { holdingsMock, portfolioSummaryMock } from '../mock/portfolioMock'
import type { Holding } from '../types/domain'

let holdings = [...holdingsMock]

export const portfolioService = {
  async getSummary() {
    return portfolioSummaryMock
  },

  async getHoldings() {
    return holdings
  },

  async addHolding(holding: Omit<Holding, 'id' | 'companyName' | 'currentPrice' | 'dayChangePct'>) {
    const created: Holding = {
      id: Date.now(),
      ticker: holding.ticker.toUpperCase(),
      companyName: `${holding.ticker.toUpperCase()} Holding`,
      shares: holding.shares,
      averageCost: holding.averageCost,
      currentPrice: holding.averageCost,
      dayChangePct: 0,
    }
    holdings = [created, ...holdings]
    return created
  },

  async updateHolding(id: number, holding: Pick<Holding, 'shares' | 'averageCost'>) {
    holdings = holdings.map((item) =>
      item.id === id
        ? {
            ...item,
            shares: holding.shares,
            averageCost: holding.averageCost,
          }
        : item,
    )
    return holdings.find((item) => item.id === id)
  },

  async deleteHolding(id: number) {
    holdings = holdings.filter((holding) => holding.id !== id)
  },
}
