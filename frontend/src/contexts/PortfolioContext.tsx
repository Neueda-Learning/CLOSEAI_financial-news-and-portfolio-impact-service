import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { portfolioService } from '../services/portfolioService'
import type { Holding, Portfolio } from '../types/domain'
import { PortfolioContext } from './portfolio'

export function PortfolioProvider({ children }: { children: ReactNode }) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([])
  const [activePortfolioId, setActivePortfolioId] = useState(1)
  const [holdings, setHoldings] = useState<Holding[]>([])
  const [summary, setSummary] = useState({
    totalValue: 0,
    totalCost: 0,
    totalPnL: 0,
    totalPnLPct: 0,
    todayChange: 0,
    todayChangePct: 0,
    allocation: [] as Array<{ ticker: string; weight: number; value: number }>,
  })

  useEffect(() => {
    refreshPortfolioState()
  }, [])

  async function refreshPortfolioState() {
    const [nextPortfolios, nextActivePortfolioId, nextHoldings, nextSummary] = await Promise.all([
      portfolioService.getPortfolios(),
      portfolioService.getActivePortfolioId(),
      portfolioService.getHoldings(),
      portfolioService.getSummary(),
    ])

    setPortfolios([...nextPortfolios])
    setActivePortfolioId(nextActivePortfolioId)
    setHoldings([...nextHoldings])
    setSummary(nextSummary)
  }

  const value = useMemo(
    () => ({
      portfolios,
      activePortfolioId,
      activePortfolio: portfolios.find((portfolio) => portfolio.id === activePortfolioId),
      holdings,
      summary,
      createPortfolio: async (name: string) => {
        await portfolioService.createPortfolio(name)
        await refreshPortfolioState()
      },
      deletePortfolio: async (id: number) => {
        await portfolioService.deletePortfolio(id)
        await refreshPortfolioState()
      },
      setActivePortfolio: async (id: number) => {
        await portfolioService.setActivePortfolio(id)
        await refreshPortfolioState()
      },
      addHolding: async (input: { ticker: string; shares: number; averageCost: number }) => {
        await portfolioService.addHolding(input)
        await refreshPortfolioState()
      },
      updateHolding: async (id: number, input: { shares: number; averageCost: number }) => {
        await portfolioService.updateHolding(id, input)
        await refreshPortfolioState()
      },
      deleteHolding: async (id: number) => {
        await portfolioService.deleteHolding(id)
        await refreshPortfolioState()
      },
    }),
    [activePortfolioId, holdings, portfolios, summary],
  )

  return <PortfolioContext.Provider value={value}>{children}</PortfolioContext.Provider>
}
