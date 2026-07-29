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
  POSITIVE: 'Positive',
  NEGATIVE: 'Negative',
  NEUTRAL: 'Neutral',
}

function minutesAgo(value: string | null) {
  if (!value) return 'Not available'
  const diffMs = Date.now() - new Date(value).getTime()
  const minutes = Math.max(1, Math.round(diffMs / 60000))
  return `${minutes} min ago`
}

function PortfolioMarketCard({
  holding,
  onEdit,
  onDelete,
}: {
  holding: Holding
  onEdit: (holding: Holding) => void
  onDelete: (id: number) => void
}) {
  const marketValue = holding.marketValue
  const totalCost = holding.totalCost
  const pnl = holding.unrealizedPnL
  const pnlPct = holding.unrealizedPnLPct
  const weight = holding.weight

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
        <span>Value <strong>{currency(marketValue)}</strong></span>
        <span>Quote <strong>{holding.quoteSource === 'CACHE' ? 'Cached / pending' : 'Latest available'}</strong></span>
      </div>
      <div className="position-grid">
        <span><small>Shares</small><strong>{holding.shares}</strong></span>
        <span><small>Average cost</small><strong>{currency(holding.averageCost)}</strong></span>
        <span><small>Total cost</small><strong>{currency(totalCost)}</strong></span>
        <span><small>Weight</small><strong>{percent(weight)}</strong></span>
        <span><small>P/L</small><strong className={pnl >= 0 ? 'positive' : 'negative'}>{pnlPct == null ? '—' : percent(pnlPct)}</strong></span>
        <span><small>Data time</small><strong>{minutesAgo(holding.quoteUpdatedAt)}</strong></span>
      </div>
      <div className="quote-status-row">
        <span className={holding.quoteSource === 'CACHE' ? 'panel-badge cache' : 'panel-badge live'}>
          {holding.quoteSource === 'CACHE' ? 'Cached quote fallback' : 'Live quote'}
        </span>
        <span>Updated {holding.quoteUpdatedAt ? dateTime(holding.quoteUpdatedAt) : 'Not available'}</span>
      </div>
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
        <SentimentBadge sentiment={event.sentiment} analysisStatus={event.analysisStatus} score={event.sentimentScore} confidence={event.confidence} />
        <span>{Math.round(event.confidence * 100)}% confidence</span>
        <span>Score {event.sentimentScore.toFixed(2)}</span>
        <span>{event.hasImpact ? `${directionLabels[event.impactDirection]} impact` : 'No portfolio impact assessed'}</span>
        <span>{event.source}</span>
        <span>{dateTime(event.publishedAt)}</span>
        <span className="ticker-tag">{event.affectedTickers.map((ticker) => `$${ticker}`).join(' ')}</span>
      </div>
      <div className="news-impact-summary">
        {event.hasImpact ? <><span className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)} price</span><span className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)} portfolio</span><span>{alignmentLabels[event.alignment]}</span></> : <span>Impact analysis pending</span>}
        <a href={event.url} target="_blank" rel="noreferrer">Original</a>
        <Link to={`/impact/${event.id}`} target="_blank" rel="noreferrer">View details</Link>
      </div>
      {event.content && <p className="news-impact-excerpt">{event.content}</p>}
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
  const [portfolioDetailsExpanded, setPortfolioDetailsExpanded] = useState(true)
  const [editingHolding, setEditingHolding] = useState<Holding | null>(null)
  const [tickerFilter, setTickerFilter] = useState('ALL')
  const [impactFilter, setImpactFilter] = useState<'ALL' | 'ANALYZED' | 'PENDING'>('ALL')
  const [newsPage, setNewsPage] = useState(1)
  const [lastRefresh, setLastRefresh] = useState('—')
  const [portfolioName, setPortfolioName] = useState('')
  const [events, setEvents] = useState<ImpactEvent[]>([])
  const [newsTotalPages, setNewsTotalPages] = useState(1)
  const [newsTotalElements, setNewsTotalElements] = useState(0)
  const negativeCount = useMemo(() => events.filter((event) => event.sentiment === 'NEGATIVE').length, [events])
  const weightTotal = summary.allocation.reduce((sum, item) => sum + item.weight, 0)
  const tickers = useMemo(() => ['ALL', ...Array.from(new Set(events.flatMap((event) => event.affectedTickers)))], [events])
  const filteredEvents = useMemo(
    () => events.filter((event) => {
      if (tickerFilter !== 'ALL' && !event.affectedTickers.includes(tickerFilter)) return false
      if (impactFilter === 'ANALYZED') return event.hasImpact || event.analysisStatus === null
      if (impactFilter === 'PENDING') return !event.hasImpact && event.analysisStatus !== null
      return true
    }),
    [events, tickerFilter, impactFilter],
  )
  const largestImpact = useMemo(
    () => Math.max(0, ...filteredEvents.map((event) => Math.abs(event.portfolioImpact))),
    [filteredEvents],
  )
  const totalPages = Math.max(1, newsTotalPages)
  const visibleEvents = filteredEvents

  useEffect(() => {
    if (!activePortfolioId) return
    impactService.getImpactEvents(activePortfolioId, newsPage).then((nextPage) => {
      setEvents(nextPage.content)
      setNewsTotalPages(nextPage.totalPages)
      setNewsTotalElements(nextPage.totalElements)
    })
  }, [activePortfolioId, newsPage])

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
    await impactService.refreshNews()
    const nextPage = await impactService.getImpactEvents(activePortfolioId, 1)
    setEvents(nextPage.content)
    setNewsTotalPages(nextPage.totalPages)
    setNewsTotalElements(nextPage.totalElements)
    setLastRefresh(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }))
    setNewsPage(1)
  }

  function cycleViewMode() {
    const currentIndex = viewSequence.indexOf(viewMode)
    const nextMode = viewSequence[(currentIndex + 1) % viewSequence.length]
    setViewMode(nextMode)
  }

  function goToNewsPage(nextPage: number) {
    setNewsPage(Math.min(totalPages, Math.max(1, Math.trunc(nextPage) || 1)))
  }

  function selectPortfolio(id: number) {
    if (id === activePortfolioId) {
      setPortfolioDetailsExpanded((expanded) => !expanded)
      setOpen(false)
      return
    }
    setOpen(false)
    setPortfolioDetailsExpanded(true)
    void setActivePortfolio(id)
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
                  const portfolioValue = portfolio.totalMarketValue ?? 0
                  const isActive = portfolio.id === activePortfolioId

                  return (
                    <div className="portfolio-list-entry" key={portfolio.id}>
                    <article className={isActive ? 'portfolio-list-item active' : 'portfolio-list-item'}>
                      <button type="button" onClick={() => selectPortfolio(portfolio.id)} aria-expanded={isActive ? portfolioDetailsExpanded : undefined}>
                        <strong>{portfolio.name}</strong>
                        <span>{currency(portfolioValue)} total value</span>
                      </button>
                      <button type="button" className="danger-text" onClick={() => deletePortfolio(portfolio.id)}>
                        Delete
                      </button>
                    </article>
                    {isActive && open && portfolioDetailsExpanded && (
                      <AddHoldingModal
                        inline
                        onClose={() => setOpen(false)}
                        onSubmit={submitHolding}
                      />
                    )}
                    </div>
                  )
                })}
              </div>
            </div>
            {portfolioDetailsExpanded && <>
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
              <span>Latest quotes updated {summary.asOf ? dateTime(summary.asOf) : 'not available'}</span>
              <span>{holdings.some((holding) => holding.quoteSource === 'CACHE') ? 'Cached fallback active' : 'Live quote feed active'}</span>
              <span>{holdings.length} holdings - weights total {weightTotal.toFixed(1)}%</span>
            </div>
            <div className="market-stack">
              {holdings.map((holding) => (
                <PortfolioMarketCard key={holding.id} holding={holding} onEdit={setEditingHolding} onDelete={deleteHolding} />
              ))}
              {holdings.length === 0 && (
                <article className="empty-state">
                  <strong>No holdings in this portfolio</strong>
                  <span>Add a ticker, quantity, and cost basis to start valuation.</span>
                </article>
              )}
            </div>
            </>}
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
                <strong className="positive">{currency(largestImpact)}</strong>
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
              <div className="impact-filter">
                {(['ALL', 'ANALYZED', 'PENDING'] as const).map((mode) => (
                  <button
                    type="button"
                    key={mode}
                    className={impactFilter === mode ? 'active' : ''}
                    onClick={() => { setImpactFilter(mode); setNewsPage(1) }}
                  >
                    {mode === 'ALL' ? 'All' : mode === 'ANALYZED' ? 'Analyzed' : 'Pending'}
                  </button>
                ))}
              </div>
              <Button variant="ghost" className="compact-button" onClick={refreshNewsNow}>Refresh news</Button>
            </div>
            <div className="news-impact-list page-turn" key={newsPage}>
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
              <button type="button" disabled={newsPage === 1} onClick={() => goToNewsPage(newsPage - 1)}>
                Previous
              </button>
              <label className="page-jump">Page <input aria-label="Jump to news page" type="number" min="1" max={totalPages} value={newsPage} onChange={(event) => goToNewsPage(Number(event.target.value))} /> of {totalPages} <span>- 20 per page</span></label>
              <button type="button" disabled={newsPage === totalPages} onClick={() => goToNewsPage(newsPage + 1)}>
                Next
              </button>
            </div>
          </div>
        </section>
      </section>

      {editingHolding && (
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
