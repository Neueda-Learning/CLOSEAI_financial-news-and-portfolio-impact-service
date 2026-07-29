import { createContext } from 'react'
import type { Holding, Portfolio } from '../types/domain'

type PortfolioRuntimeSummary = {
  totalValue: number
  totalCost: number
  totalPnL: number
  totalPnLPct: number
  todayChange: number
  todayChangePct: number
  allocation: Array<{ ticker: string; weight: number; value: number }>
}

export type PortfolioContextValue = {
  portfolios: Portfolio[]
  activePortfolioId: number
  activePortfolio: Portfolio | undefined
  holdings: Holding[]
  summary: PortfolioRuntimeSummary
  createPortfolio: (name: string) => Promise<void>
  deletePortfolio: (id: number) => Promise<void>
  setActivePortfolio: (id: number) => Promise<void>
  addHolding: (input: { ticker: string; shares: number; averageCost: number }) => Promise<void>
  updateHolding: (id: number, input: { shares: number; averageCost: number }) => Promise<void>
  deleteHolding: (id: number) => Promise<void>
}

export const PortfolioContext = createContext<PortfolioContextValue | null>(null)
