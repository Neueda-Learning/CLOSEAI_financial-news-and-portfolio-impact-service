import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { portfolioService } from '../services/portfolioService'
import type { Holding } from '../types/domain'
import { PortfolioContext } from './portfolio'

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [holdings, setHoldings] = useState<Holding[]>([])

  useEffect(() => {
    portfolioService.getHoldings().then(setHoldings)
  }, [])

  const value = useMemo(
    () => ({
      holdings,
      addHolding: async (input: { ticker: string; shares: number; averageCost: number }) => {
        await portfolioService.addHolding(input)
        setHoldings(await portfolioService.getHoldings())
      },
      updateHolding: async (id: number, input: { shares: number; averageCost: number }) => {
        await portfolioService.updateHolding(id, input)
        setHoldings(await portfolioService.getHoldings())
      },
      deleteHolding: async (id: number) => {
        await portfolioService.deleteHolding(id)
        setHoldings(await portfolioService.getHoldings())
      },
    }),
    [holdings],
  )

  return <PortfolioContext.Provider value={value}>{children}</PortfolioContext.Provider>
}
