import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { AddHoldingModal } from '../components/portfolio/AddHoldingModal'
import { Button } from '../components/common/Button'
import { SentimentBadge } from '../components/impact/SentimentBadge'
import { impactEventsMock } from '../mock/impactMock'
import { portfolioSummaryMock } from '../mock/portfolioMock'
import { usePortfolio } from '../hooks/usePortfolio'
import { currency, dateTime, percent } from '../utils/formatters'
import type { Holding, ImpactEvent } from '../types/domain'

type ViewMode = 'both' | 'portfolio' | 'news'

function sparklinePath(values: number[]) {
  const width = 320
  const height = 58
  const padding = 4
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1

  return values
    .map((value, index) => {
      const x = padding + (index / (values.length - 1)) * (width - padding * 2)
      const y = height - padding - ((value - min) / range) * (height - padding * 2)
      return `${x},${y}`
    })
    .join(' ')
}

function trendForHolding(holding: Holding) {
  const base = holding.currentPrice
  const direction = holding.dayChangePct >= 0 ? 1 : -1
  return [base * (1 - direction * 0.025), base * (1 - direction * 0.012), base * (1 + direction * 0.004), base * (1 - direction * 0.006), base * (1 + direction * 0.016), base]
}

function PortfolioMarketCard({ holding }: { holding: Holding }) {
  const marketValue = holding.currentPrice * holding.shares
  const totalCost = holding.averageCost * holding.shares
  const pnl = marketValue - totalCost
  const high = holding.currentPrice * 1.018
  const low = holding.currentPrice * 0.984
  const trend = trendForHolding(holding)
  const color = holding.dayChangePct >= 0 ? '#2f7d63' : '#b91c1c'

  return (
    <article className="market-card">
      <div className="market-card-top">
        <div>
          <div className="stock-symbol">${holding.ticker}</div>
          <div className="stock-name">{holding.companyName} · NASDAQ</div>
        </div>
        <span className={holding.dayChangePct >= 0 ? 'stock-change up' : 'stock-change down'}>{percent(holding.dayChangePct)}</span>
      </div>
      <div className="stock-price-row">
        <span className="stock-price">{holding.currentPrice.toFixed(2)}</span>
        <span className={pnl >= 0 ? 'positive' : 'negative'}>{currency(pnl)} unrealized</span>
      </div>
      <div className="stock-meta">
        <span>High <strong>{high.toFixed(2)}</strong></span>
        <span>Low <strong>{low.toFixed(2)}</strong></span>
        <span>Value <strong>{currency(marketValue)}</strong></span>
      </div>
      <svg className="mini-chart" viewBox="0 0 320 58" aria-label={`${holding.ticker} intraday trend`}>
        <polyline points={sparklinePath(trend)} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </article>
  )
}

function NewsImpactItem({ event }: { event: ImpactEvent }) {
  return (
    <article className={`news-impact-item ${event.sentiment.toLowerCase()}`}>
      <div className="news-impact-headline">{event.headline}</div>
      <div className="news-impact-meta">
        <SentimentBadge sentiment={event.sentiment} />
        <span>{Math.round(event.confidence * 100)}% confidence</span>
        <span>{event.source}</span>
        <span>{dateTime(event.publishedAt)}</span>
        <span className="ticker-tag">${event.ticker}</span>
      </div>
      <div className="news-impact-summary">
        <span className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)} price</span>
        <span className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)} portfolio</span>
        <Link to={`/impact/${event.id}`}>View analysis</Link>
      </div>
    </article>
  )
}

export function PortfolioImpactPage() {
  const { holdings, addHolding } = usePortfolio()
  const [viewMode, setViewMode] = useState<ViewMode>('both')
  const [open, setOpen] = useState(false)
  const negativeCount = useMemo(() => impactEventsMock.filter((event) => event.sentiment === 'NEGATIVE').length, [])

  return (
    <div className={`split-module split-${viewMode}`}>
      <header className="module-topbar">
        <div>
          <p className="eyebrow">Portfolio Impact</p>
          <h1>Holdings and news impact, side by side</h1>
          <p className="lede">Left page tracks the portfolio. Right page explains which headlines are moving it.</p>
        </div>
        <div className="view-switcher" aria-label="Choose split view">
          <button className={viewMode === 'portfolio' ? 'active' : ''} onClick={() => setViewMode('portfolio')}>Portfolio full</button>
          <button className={viewMode === 'both' ? 'active' : ''} onClick={() => setViewMode('both')}>Both pages</button>
          <button className={viewMode === 'news' ? 'active' : ''} onClick={() => setViewMode('news')}>News full</button>
        </div>
      </header>

      <section className="split-pages">
        <section className="split-pane portfolio-pane" aria-label="Portfolio page">
          <div className="panel-header sticky-panel-header">
            <div>
              <span className="panel-title">Module B · Portfolio Market Data</span>
              <p>Live-style prices, holdings value, and intraday movement.</p>
            </div>
            <span className="panel-badge live"><span className="status-dot green" />Mock price cache</span>
          </div>
          <div className="split-scroll">
            <div className="pane-summary">
              <div>
                <small>Total Value</small>
                <strong>{currency(portfolioSummaryMock.totalValue)}</strong>
              </div>
              <div>
                <small>Today</small>
                <strong className={portfolioSummaryMock.todayChange >= 0 ? 'positive' : 'negative'}>{percent(portfolioSummaryMock.todayChangePct)}</strong>
              </div>
              <Button onClick={() => setOpen(true)}>+ Add Holding</Button>
            </div>
            <div className="market-stack">
              {holdings.map((holding) => (
                <PortfolioMarketCard key={holding.id} holding={holding} />
              ))}
            </div>
          </div>
        </section>

        <section className="split-pane news-pane" aria-label="News impact page">
          <div className="panel-header sticky-panel-header">
            <div>
              <span className="panel-title">Module C · News Impact</span>
              <p>Sentiment, confidence, price movement, and portfolio contribution.</p>
            </div>
            <span className="panel-badge polling">{negativeCount} risk headlines</span>
          </div>
          <div className="split-scroll">
            <div className="pane-summary news-summary">
              <div>
                <small>Events</small>
                <strong>{impactEventsMock.length}</strong>
              </div>
              <div>
                <small>Largest impact</small>
                <strong className="positive">{currency(1614)}</strong>
              </div>
              <div>
                <small>Next poll</small>
                <strong>1:45</strong>
              </div>
            </div>
            <div className="news-impact-list">
              {impactEventsMock.map((event) => (
                <NewsImpactItem key={event.id} event={event} />
              ))}
            </div>
          </div>
        </section>
      </section>

      {open && <AddHoldingModal onClose={() => setOpen(false)} onSubmit={addHolding} />}
    </div>
  )
}
