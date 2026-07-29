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

const viewSequence: ViewMode[] = ['both', 'portfolio', 'news']

const viewLabels: Record<ViewMode, string> = {
  both: 'Both pages',
  portfolio: 'Portfolio full',
  news: 'News full',
}

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

function PortfolioMarketCard({
  holding,
  totalValue,
  onEdit,
  onDelete,
}: {
  holding: Holding
  totalValue: number
  onEdit: (holding: Holding) => void
  onDelete: (id: number) => void
}) {
  const marketValue = holding.currentPrice * holding.shares
  const totalCost = holding.averageCost * holding.shares
  const pnl = marketValue - totalCost
  const pnlPct = totalCost === 0 ? 0 : (pnl / totalCost) * 100
  const weight = totalValue === 0 ? 0 : (marketValue / totalValue) * 100
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
        <div className="market-actions">
          <span className={holding.dayChangePct >= 0 ? 'stock-change up' : 'stock-change down'}>{percent(holding.dayChangePct)}</span>
          <button type="button" onClick={() => onEdit(holding)}>Edit</button>
          <button type="button" className="danger-text" onClick={() => onDelete(holding.id)}>Delete</button>
        </div>
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
      <div className="position-grid">
        <span><small>Shares</small><strong>{holding.shares}</strong></span>
        <span><small>Average cost</small><strong>{currency(holding.averageCost)}</strong></span>
        <span><small>Total cost</small><strong>{currency(totalCost)}</strong></span>
        <span><small>Weight</small><strong>{percent(weight)}</strong></span>
        <span><small>P/L</small><strong className={pnl >= 0 ? 'positive' : 'negative'}>{percent(pnlPct)}</strong></span>
      </div>
      <svg className="mini-chart" viewBox="0 0 320 58" aria-label={`${holding.ticker} intraday trend`}>
        <polyline points={sparklinePath(trend)} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </article>
  )
}

function NewsImpactItem({ event }: { event: ImpactEvent }) {
  const alignmentLabels: Record<NonNullable<ImpactEvent['alignment']>, string> = {
    CONFIRMED: 'Direction confirmed',
    DIVERGENT: 'Divergent move',
    INCONCLUSIVE: 'Inconclusive',
  }
  const isPending = event.sentiment === null
  const sentimentTone = isPending ? 'pending' : event.sentiment === 'POSITIVE' ? 'positive' : event.sentiment === 'NEGATIVE' ? 'negative' : 'neutral'

  return (
    <article className={`news-impact-item ${sentimentTone}`}>
      <Link className="news-impact-headline-link" to={`/impact/${event.id}`} target="_blank" rel="noreferrer">
        <div className="news-impact-headline">{event.headline}</div>
      </Link>
      <div className="news-impact-meta">
        <SentimentBadge sentiment={event.sentiment} score={event.sentimentScore} confidence={event.confidence} />
        <span>{event.confidence === null ? 'Pending analysis' : `${Math.round(event.confidence * 100)}% confidence`}</span>
        <span>{event.sentimentScore === null ? 'Score pending' : `Score ${event.sentimentScore.toFixed(2)}`}</span>
        <span>{event.source}</span>
        <span>{dateTime(event.publishedAt)}</span>
        <span className="ticker-tag">${event.ticker}</span>
      </div>
      <div className="news-impact-summary">
        <span className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)} price</span>
        <span className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)} portfolio</span>
        <span>{event.alignment === null ? 'Analyzing alignment' : alignmentLabels[event.alignment]}</span>
        <a href={event.url} target="_blank" rel="noreferrer">Original</a>
        <Link to={`/impact/${event.id}`} target="_blank" rel="noreferrer">View details</Link>
      </div>
      <p className="news-impact-excerpt">{event.content}</p>
    </article>
  )
}

export function PortfolioImpactPage() {
  const { holdings, addHolding, updateHolding, deleteHolding } = usePortfolio()
  const [viewMode, setViewMode] = useState<ViewMode>('both')
  const [open, setOpen] = useState(false)
  const [editingHolding, setEditingHolding] = useState<Holding | null>(null)
  const [tickerFilter, setTickerFilter] = useState('ALL')
  const [newsPage, setNewsPage] = useState(1)
  const [lastRefresh, setLastRefresh] = useState('12:31')
  const negativeCount = useMemo(() => impactEventsMock.filter((event) => event.sentiment === 'NEGATIVE').length, [])
  const totalValue = useMemo(() => holdings.reduce((sum, holding) => sum + holding.currentPrice * holding.shares, 0), [holdings])
  const totalCost = useMemo(() => holdings.reduce((sum, holding) => sum + holding.averageCost * holding.shares, 0), [holdings])
  const tickers = useMemo(() => ['ALL', ...Array.from(new Set(impactEventsMock.map((event) => event.ticker)))], [])
  const filteredEvents = useMemo(
    () => impactEventsMock.filter((event) => tickerFilter === 'ALL' || event.ticker === tickerFilter),
    [tickerFilter],
  )
  const pageSize = 20
  const totalPages = Math.max(1, Math.ceil(filteredEvents.length / pageSize))
  const visibleEvents = filteredEvents.slice((newsPage - 1) * pageSize, newsPage * pageSize)

  async function submitHolding(input: { ticker: string; shares: number; averageCost: number }) {
    if (editingHolding) {
      await updateHolding(editingHolding.id, { shares: input.shares, averageCost: input.averageCost })
      setEditingHolding(null)
      return
    }

    await addHolding(input)
  }

  function refreshNewsNow() {
    setLastRefresh(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }))
    setNewsPage(1)
  }

  function cycleViewMode() {
    const currentIndex = viewSequence.indexOf(viewMode)
    const nextMode = viewSequence[(currentIndex + 1) % viewSequence.length]
    setViewMode(nextMode)
  }

  return (
    <div className={`split-module split-${viewMode}`}>
      <header className="module-topbar">
        <div>
          <p className="eyebrow">Portfolio Impact</p>
          <h1>Holdings and news impact, side by side</h1>
        </div>
        <button className="view-cycle-button" onClick={cycleViewMode} aria-label={`Current view ${viewLabels[viewMode]}. Click to switch view.`}>
          <span className={`view-cycle-glyph mode-${viewMode}`} aria-hidden="true">
            <i />
            <i />
          </span>
          <span>
            <small>View</small>
            {viewLabels[viewMode]}
          </span>
        </button>
      </header>

      <section className="split-pages">
        <section className="split-pane portfolio-pane" aria-label="Portfolio page">
          <div className="panel-header sticky-panel-header">
            <div>
              <span className="panel-title">Portfolio Market Data</span>
              <p>Live-style prices, holdings value, and intraday movement.</p>
            </div>
            <span className="panel-badge live"><span className="status-dot green" />Mock price cache</span>
          </div>
          <div className="split-scroll">
            <div className="pane-summary">
              <div>
                <small>Total Value</small>
                <strong>{currency(totalValue || portfolioSummaryMock.totalValue)}</strong>
              </div>
              <div>
                <small>Total Cost</small>
                <strong>{currency(totalCost)}</strong>
              </div>
              <Button onClick={() => setOpen(true)}>+ Add Holding</Button>
            </div>
            <div className="portfolio-service-strip">
              <span>Latest quotes updated 4 minutes ago</span>
              <span>Cached fallback enabled</span>
              <span>{holdings.length} holdings · weights recalculated</span>
            </div>
            <div className="market-stack">
              {holdings.map((holding) => (
                <PortfolioMarketCard key={holding.id} holding={holding} totalValue={totalValue} onEdit={setEditingHolding} onDelete={deleteHolding} />
              ))}
            </div>
          </div>
        </section>

        <section className="split-pane news-pane" aria-label="News impact page">
          <div className="panel-header sticky-panel-header">
            <div>
              <span className="panel-title">News Impact</span>
              <p>Sentiment, confidence, price movement, and portfolio contribution.</p>
            </div>
            <span className="panel-badge polling">{negativeCount} risk headlines</span>
          </div>
          <div className="split-scroll">
            <div className="pane-summary news-summary">
              <div>
                <small>Events</small>
                <strong>{filteredEvents.length}</strong>
              </div>
              <div>
                <small>Largest impact</small>
                <strong className="positive">{currency(1614)}</strong>
              </div>
              <div>
                <small>Last refresh</small>
                <strong>{lastRefresh}</strong>
              </div>
            </div>
            <div className="news-controls" aria-label="News filters and refresh controls">
              <div className="ticker-filter">
                {tickers.map((ticker) => (
                  <button
                    type="button"
                    key={ticker}
                    className={tickerFilter === ticker ? 'active' : ''}
                    onClick={() => {
                      setTickerFilter(ticker)
                      setNewsPage(1)
                    }}
                  >
                    {ticker === 'ALL' ? 'All' : `$${ticker}`}
                  </button>
                ))}
              </div>
              <Button variant="ghost" className="compact-button" onClick={refreshNewsNow}>Refresh news</Button>
            </div>
            <div className="news-impact-list">
              {visibleEvents.map((event) => (
                <NewsImpactItem key={event.id} event={event} />
              ))}
            </div>
            <div className="pagination-row">
              <button type="button" disabled={newsPage === 1} onClick={() => setNewsPage((page) => Math.max(1, page - 1))}>
                Previous
              </button>
              <span>Page {newsPage} of {totalPages}</span>
              <button type="button" disabled={newsPage === totalPages} onClick={() => setNewsPage((page) => Math.min(totalPages, page + 1))}>
                Next
              </button>
            </div>
          </div>
        </section>
      </section>

      {(open || editingHolding) && (
        <AddHoldingModal
          mode={editingHolding ? 'edit' : 'add'}
          initialValue={editingHolding ?? undefined}
          onClose={() => {
            setOpen(false)
            setEditingHolding(null)
          }}
          onSubmit={submitHolding}
        />
      )}
    </div>
  )
}
