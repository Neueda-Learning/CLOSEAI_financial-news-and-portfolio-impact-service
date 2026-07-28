import { createContext } from 'react'
import type { Holding } from '../types/domain'

export type PortfolioContextValue = {
  holdings: Holding[]
  addHolding: (input: { ticker: string; shares: number; averageCost: number }) => Promise<void>
  deleteHolding: (id: number) => Promise<void>
}

export const PortfolioContext = createContext<PortfolioContextValue | null>(null)
