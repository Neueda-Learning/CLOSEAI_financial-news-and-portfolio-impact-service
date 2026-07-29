import { useState } from 'react'
import type { CSSProperties, PointerEvent } from 'react'
import { dateTime } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'
import { SentimentBadge } from './SentimentBadge'

const brandSurface: Record<string, { logo: string; tone: string }> = {
  AAPL: { logo: 'https://cdn.simpleicons.org/apple/29435a', tone: '#9db9d3' },
  NVDA: { logo: 'https://cdn.simpleicons.org/nvidia/29435a', tone: '#a5c6b4' },
  MSFT: { logo: 'https://cdn.simpleicons.org/microsoft/29435a', tone: '#b4c2dc' },
  AMD: { logo: 'https://cdn.simpleicons.org/amd/29435a', tone: '#d1b5b5' },
}

export function NewsInfoCard({ event }: { event: ImpactEvent }) {
  const [pointer, setPointer] = useState({ x: '50%', y: '50%', active: 0 })
  const brand = brandSurface[event.ticker] ?? { logo: 'https://cdn.simpleicons.org/stock/29435a', tone: '#b7c6d7' }
  const alignmentLabel = event.alignment === 'CONFIRMED' ? 'confirmed' : event.alignment === 'DIVERGENT' ? 'divergent' : 'inconclusive'
  const directionLabels: Record<ImpactEvent['impactDirection'], string> = {
    POSITIVE: 'Positive',
    NEGATIVE: 'Negative',
    NEUTRAL: 'Neutral',
  }
  const impactAmount = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(event.portfolioImpact)
  const surfaceStyle = {
    '--news-pointer-x': pointer.x,
    '--news-pointer-y': pointer.y,
    '--news-pointer-active': pointer.active,
    '--news-brand-tone': brand.tone,
  } as CSSProperties
  const brandImage = `linear-gradient(120deg, rgba(248, 251, 255, .82), rgba(229, 238, 247, .58)), url("${brand.logo}")`

  function handlePointerMove(nextPointer: PointerEvent<HTMLElement>) {
    const bounds = nextPointer.currentTarget.getBoundingClientRect()
    const x = nextPointer.clientX - bounds.left
    const y = nextPointer.clientY - bounds.top
    const edgeDistance = Math.min(x, y, bounds.width - x, bounds.height - y)
    const edgeFocus = Math.min(1, Math.max(0, (450 - edgeDistance) / 450))

    setPointer({
      x: `${x}px`,
      y: `${y}px`,
      active: Number(edgeFocus.toFixed(2)),
    })
  }

  return (
    <Card
      className="news-info-card"
      style={surfaceStyle}
      onPointerMove={handlePointerMove}
      onPointerLeave={() => setPointer((current) => ({ ...current, active: 0 }))}
    >
      <div className="news-info-card-backdrop" style={{ backgroundImage: brandImage }} aria-hidden="true" />
      <div className="news-info-card-shade" aria-hidden="true" />
      <div className="news-info-card-content">
        <div className="news-info-card-kicker">
          <p className="eyebrow">News Information</p>
          <span>{event.affectedTickers.map((ticker) => `$${ticker}`).join(' ')}</span>
        </div>
        <h2>{event.headline}</h2>
        <div className="news-info-grid">
          <div className="news-info-cell">
            <span>Original</span>
            <a className="inline-link" href={event.url} target="_blank" rel="noreferrer">Open article</a>
          </div>
          <div className="news-info-cell">
            <span>Published</span>
            <b>{dateTime(event.publishedAt)}</b>
          </div>
          <div className="news-info-flow">
            <div>
              <small>Document flow</small>
              <strong>{event.sentiment ?? (event.analysisStatus === 'FAILED' ? 'analysis failed' : 'analysis pending')} - {alignmentLabel} by market move</strong>
            </div>
            <p>
              The marked event point at {event.priceSeries[2]?.time ?? 'the same session'} maps to {event.affectedTickers.join(', ')} with a {directionLabels[event.impactDirection].toLowerCase()} direction, then compares the same-day price path to calculate
              {event.portfolioImpact >= 0 ? ' a gain of ' : ' a loss of '}
              <span className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{impactAmount}</span>
              .
            </p>
          </div>
          <div className="news-info-cell">
            <span>Source</span>
            <b>{event.source}</b>
          </div>
          <div className="news-info-cell">
            <span>Sentiment</span>
            <SentimentBadge sentiment={event.sentiment} analysisStatus={event.analysisStatus} score={event.sentimentScore} confidence={event.confidence} />
          </div>
          <div className="news-info-cell">
            <span>Score</span>
            <b>{event.sentimentScore.toFixed(2)}</b>
          </div>
          <div className="news-info-cell">
            <span>Confidence</span>
            <b>{Math.round(event.confidence * 100)}%</b>
          </div>
        </div>
      </div>
    </Card>
  )
}
