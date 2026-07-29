import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AddHoldingModal } from '../components/portfolio/AddHoldingModal'
import { Button } from '../components/common/Button'
import { SentimentBadge } from '../components/impact/SentimentBadge'
import { impactService } from '../services/impactService'
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

const directionLabels: Record<ImpactEvent['impactDirection'], string> = {
  BULLISH: 'Bullish',
  BEARISH: 'Bearish',
  NEUTRAL: 'Neutral',
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

function minutesAgo(value: string) {
  const diffMs = Date.now() - new Date(value).getTime()
  const minutes = Math.max(1, Math.round(diffMs / 60000))
  return `${minutes} min ago`
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
          <div className="stock-name">{holding.companyName} - NASDAQ</div>
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
        <span><small>Data time</small><strong>{minutesAgo(holding.quoteUpdatedAt)}</strong></span>
      </div>
      <div className="quote-status-row">
        <span className={holding.quoteSource === 'CACHE' ? 'panel-badge cache' : 'panel-badge live'}>
          {holding.quoteSource === 'CACHE' ? 'Cached quote fallback' : 'Live quote'}
        </span>
        <span>Updated {dateTime(holding.quoteUpdatedAt)}</span>
      </div>
      <svg className="mini-chart" viewBox="0 0 320 58" aria-label={`${holding.ticker} intraday trend`}>
        <polyline points={sparklinePath(trend)} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </article>
  )
}

function NewsImpactItem({ event }: { event: ImpactEvent }) {
  const alignmentLabels: Record<ImpactEvent['alignment'], string> = {
    CONFIRMED: 'Direction confirmed',
    DIVERGENT: 'Divergent move',
    INCONCLUSIVE: 'Inconclusive',
  }
  const sentimentTone = event.sentiment === 'POSITIVE' ? 'positive' : event.sentiment === 'NEGATIVE' ? 'negative' : 'neutral'

  return (
    <article className={`news-impact-item ${sentimentTone}`}>
      <Link className="news-impact-headline-link" to={`/impact/${event.id}`} target="_blank" rel="noreferrer">
        <div className="news-impact-headline">{event.headline}</div>
      </Link>
      <div className="news-impact-meta">
        <SentimentBadge sentiment={event.sentiment} score={event.sentimentScore} confidence={event.confidence} />
        <span>{Math.round(event.confidence * 100)}% confidence</span>
        <span>Score {event.sentimentScore.toFixed(2)}</span>
        <span>{directionLabels[event.impactDirection]} impact</span>
        <span>{event.source}</span>
        <span>{dateTime(event.publishedAt)}</span>
        <span className="ticker-tag">{event.affectedTickers.map((ticker) => `$${ticker}`).join(' ')}</span>
      </div>
      <div className="news-impact-summary">
        <span className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)} price</span>
        <span className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)} portfolio</span>
        <span>{alignmentLabels[event.alignment]}</span>
        <a href={event.url} target="_blank" rel="noreferrer">Original</a>
        <Link to={`/impact/${event.id}`} target="_blank" rel="noreferrer">View details</Link>
      </div>
      <p className="news-impact-excerpt">{event.content}</p>
    </article>
  )
}

export function PortfolioImpactPage() {
  const {
    portfolios,
    activePortfolioId,
    activePortfolio,
    holdings,
    summary,
    createPortfolio,
    deletePortfolio,
    setActivePortfolio,
    addHolding,
    updateHolding,
    deleteHolding,
  } = usePortfolio()
  const [viewMode, setViewMode] = useState<ViewMode>('both')
  const [open, setOpen] = useState(false)
  const [editingHolding, setEditingHolding] = useState<Holding | null>(null)
  const [tickerFilter, setTickerFilter] = useState('ALL')
  const [newsPage, setNewsPage] = useState(1)
  const [lastRefresh, setLastRefresh] = useState('12:31')
  const [portfolioName, setPortfolioName] = useState('')
  const [events, setEvents] = useState<ImpactEvent[]>([])
  const activeHoldingTickers = useMemo(() => holdings.map((holding) => holding.ticker), [holdings])
  const negativeCount = useMemo(() => events.filter((event) => event.sentiment === 'NEGATIVE').length, [events])
  const weightTotal = summary.allocation.reduce((sum, item) => sum + item.weight, 0)
  const tickers = useMemo(() => ['ALL', ...Array.from(new Set(events.flatMap((event) => event.affectedTickers)))], [events])
  const filteredEvents = useMemo(
    () => events.filter((event) => tickerFilter === 'ALL' || event.affectedTickers.includes(tickerFilter)),
    [events, tickerFilter],
  )
  const pageSize = 20
  const totalPages = Math.max(1, Math.ceil(filteredEvents.length / pageSize))
  const visibleEvents = filteredEvents.slice((newsPage - 1) * pageSize, newsPage * pageSize)

  useEffect(() => {
    impactService.getImpactEventsForTickers(activeHoldingTickers).then((nextEvents) => {
      setEvents(nextEvents)
      setNewsPage(1)
    })
  }, [activeHoldingTickers])

  async function submitHolding(input: { ticker: string; shares: number; averageCost: number }) {
    if (editingHolding) {
      await updateHolding(editingHolding.id, { shares: input.shares, averageCost: input.averageCost })
      setEditingHolding(null)
      return
    }

    await addHolding(input)
  }

  async function submitPortfolio(event: FormEvent) {
    event.preventDefault()
    await createPortfolio(portfolioName)
    setPortfolioName('')
  }

  async function refreshNewsNow() {
    setEvents(await impactService.getImpactEventsForTickers(activeHoldingTickers))
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
            <span className="panel-badge live"><span className="status-dot green" />Quote monitor</span>
          </div>
          <div className="split-scroll">
            <div className="portfolio-management">
              <form className="portfolio-create-form" onSubmit={submitPortfolio}>
                <label>
                  <span>Create portfolio</span>
                  <input value={portfolioName} onChange={(event) => setPortfolioName(event.target.value)} placeholder="Income strategy" required />
                </label>
                <Button type="submit" className="compact-button">Create</Button>
              </form>
              <div className="portfolio-list" aria-label="All portfolios">
                {portfolios.map((portfolio) => {
                  const portfolioValue = portfolio.holdings.reduce((sum, holding) => sum + holding.currentPrice * holding.shares, 0)
                  const isActive = portfolio.id === activePortfolioId

                  return (
                    <article className={isActive ? 'portfolio-list-item active' : 'portfolio-list-item'} key={portfolio.id}>
                      <button type="button" onClick={() => setActivePortfolio(portfolio.id)}>
                        <strong>{portfolio.name}</strong>
                        <span>{currency(portfolioValue)} total value</span>
                      </button>
                      <button type="button" className="danger-text" onClick={() => deletePortfolio(portfolio.id)} disabled={portfolios.length === 1}>
                        Delete
                      </button>
                    </article>
                  )
                })}
              </div>
            </div>
            <div className="pane-summary">
              <div>
                <small>{activePortfolio?.name ?? 'Selected portfolio'}</small>
                <strong>{currency(summary.totalValue)}</strong>
              </div>
              <div>
                <small>Total Cost</small>
                <strong>{currency(summary.totalCost)}</strong>
              </div>
              <div>
                <small>Total P/L</small>
                <strong className={summary.totalPnL >= 0 ? 'positive' : 'negative'}>{currency(summary.totalPnL)} / {percent(summary.totalPnLPct)}</strong>
              </div>
              <Button onClick={() => setOpen(true)}>+ Add Holding</Button>
            </div>
            <div className="portfolio-service-strip">
              <span>Latest quotes updated {holdings.length === 0 ? 'not available' : '4 minutes ago'}</span>
              <span>{holdings.some((holding) => holding.quoteSource === 'CACHE') ? 'Cached fallback active' : 'Live quote feed active'}</span>
              <span>{holdings.length} holdings - weights total {weightTotal.toFixed(1)}%</span>
            </div>
            <div className="market-stack">
              {holdings.map((holding) => (
                <PortfolioMarketCard key={holding.id} holding={holding} totalValue={summary.totalValue} onEdit={setEditingHolding} onDelete={deleteHolding} />
              ))}
              {holdings.length === 0 && (
                <article className="empty-state">
                  <strong>No holdings in this portfolio</strong>
                  <span>Add a ticker, quantity, and cost basis to start valuation.</span>
                </article>
              )}
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
                <strong className="positive">{currency(Math.max(0, ...filteredEvents.map((event) => Math.abs(event.portfolioImpact))))}</strong>
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
                <NewsImpactItem key={event.externalId} event={event} />
              ))}
              {visibleEvents.length === 0 && (
                <article className="empty-state">
                  <strong>No matched news</strong>
                  <span>Add holdings with supported tickers to populate the impact stream.</span>
                </article>
              )}
            </div>
            <div className="pagination-row">
              <button type="button" disabled={newsPage === 1} onClick={() => setNewsPage((page) => Math.max(1, page - 1))}>
                Previous
              </button>
              <span>Page {newsPage} of {totalPages} - 20 per page</span>
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
