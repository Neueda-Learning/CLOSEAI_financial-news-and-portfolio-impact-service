import { useEffect, useMemo, useState } from 'react'
import { Card } from '../components/common/Card'
import { LineChart } from '../components/charts/LineChart'
import { PieChart } from '../components/charts/PieChart'
import { impactService } from '../services/impactService'
import { portfolioService } from '../services/portfolioService'
import { usePortfolio } from '../hooks/usePortfolio'
import { currency, dateTime, percent } from '../utils/formatters'

type ImpactSummary = Awaited<ReturnType<typeof impactService.getImpactSummary>>

export function DashboardPage({ themeVariant }: { themeVariant?: 'forest' } = {}) {
  const { activePortfolio, activePortfolioId, holdings, summary } = usePortfolio()
  const [history, setHistory] = useState<Array<{ date: string; totalValue: number }>>([])
  const [impactSummary, setImpactSummary] = useState<ImpactSummary | null>(null)

  useEffect(() => {
    if (!activePortfolioId) return
    void Promise.all([portfolioService.getValuationHistory(activePortfolioId), impactService.getImpactSummary(activePortfolioId)])
      .then(([nextHistory, nextImpact]) => { setHistory(nextHistory); setImpactSummary(nextImpact) })
      .catch(() => { setHistory([]); setImpactSummary(null) })
  }, [activePortfolioId])

  const stockAllocation = summary.allocation
  const weightTotal = stockAllocation.reduce((sum, item) => sum + item.weight, 0)
  const counts = impactSummary?.counts
  const agreementRate = impactSummary?.directionAgreementRate == null ? null : impactSummary.directionAgreementRate * 100
  const coverage = impactSummary?.newsCoverage == null ? null : impactSummary.newsCoverage * 100
  const weightedSentiment = impactSummary?.weightedSentiment ?? 0
  const holdingTickerSet = useMemo(() => new Set(holdings.map((holding) => holding.ticker)), [holdings])
  const isForestTheme = themeVariant === 'forest'
  const allocationColors = isForestTheme
    ? ['#b3c9b6', '#7d977f', '#526951', '#2c3e2b', '#8fa993', '#d0dfd2']
    : undefined

  return (
    <div className="page-stack">
      <header className="hero"><div><p className="eyebrow">{isForestTheme ? 'Forest palette preview' : 'Dashboard'}</p><h1>{activePortfolio?.name ?? 'Portfolio health'}</h1></div><Card className="hero-metric"><small>Total value</small><strong>{currency(summary.totalValue)}</strong><span className={summary.todayChange >= 0 ? 'positive' : 'negative'}>{percent(summary.todayChangePct)} today</span></Card></header>
      <section className="signal-row" aria-label="Portfolio summary">
        <Card className="signal-card interactive-signal-card"><small>Today's Change</small><strong className={summary.todayChange >= 0 ? 'positive' : 'negative'}>{currency(summary.todayChange)}</strong><span>{percent(summary.todayChangePct)}</span></Card>
        <Card className="signal-card interactive-signal-card"><small>Total P/L</small><strong className={summary.totalPnL >= 0 ? 'positive' : 'negative'}>{currency(summary.totalPnL)}</strong><span>{percent(summary.totalPnLPct)}</span></Card>
        <Card className="signal-card interactive-signal-card"><small>Tracked Symbols</small><strong>{holdingTickerSet.size}</strong><span>Active holdings</span></Card>
      </section>
      <Card className="impact-summary-overview"><div className="impact-summary-main"><small>Direction agreement</small><strong>{agreementRate == null ? 'Sample insufficient' : `${Math.round(agreementRate)}%`}</strong><div className="agreement-bar" aria-hidden="true"><span style={{ width: `${agreementRate ?? 0}%` }} /></div><p>{impactSummary ? `Based on ${impactSummary.sampleSize} assessed records.` : 'Impact data is not available yet.'}</p></div><div className="impact-summary-counts"><span className="alignment-confirmed">Confirmed {counts?.confirmed ?? 0}</span><span className="alignment-divergent">Divergent {counts?.divergent ?? 0}</span><span className="alignment-inconclusive">Inconclusive {counts?.inconclusive ?? 0}</span></div><div className="impact-summary-metrics"><div><small>Weighted sentiment</small><strong className={weightedSentiment >= 0 ? 'positive' : 'negative'}>{weightedSentiment.toFixed(2)}</strong></div><div><small>News coverage</small><strong>{coverage == null ? '—' : `${Math.round(coverage)}%`}</strong></div><div><small>Data as of</small><strong>{summary.asOf ? dateTime(summary.asOf) : 'Pending'}</strong></div></div></Card>
      <section className="grid-2 dashboard-charts"><Card><h2>Portfolio Allocation</h2><div className="chart-box"><PieChart labels={stockAllocation.map((item) => item.ticker)} values={stockAllocation.map((item) => item.value)} colors={allocationColors} /></div><p className="chart-footnote">Weights total {weightTotal.toFixed(1)}% across stock holdings.</p></Card><Card><h2>Portfolio Historical Value</h2><div className="chart-box"><LineChart labels={history.map((item) => item.date)} datasets={[{ label: 'Portfolio value', data: history.map((item) => item.totalValue), borderColor: isForestTheme ? '#b3c9b6' : '#43718e', backgroundColor: isForestTheme ? 'rgba(179, 201, 182, 0.16)' : 'rgba(67, 113, 142, 0.14)' }]} /></div></Card></section>
      <Card><h2>Holdings Summary</h2><div className="table-wrap compact"><table><thead><tr><th>Ticker</th><th>Weight</th><th>Value</th></tr></thead><tbody>{stockAllocation.map((item) => <tr key={item.ticker}><td className="ticker">{item.ticker}</td><td>{item.weight.toFixed(1)}%</td><td>{currency(item.value)}</td></tr>)}</tbody></table></div></Card>
    </div>
  )
}
