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
  const surfaceStyle = {
    '--news-pointer-x': pointer.x,
    '--news-pointer-y': pointer.y,
    '--news-pointer-active': pointer.active,
    '--news-brand-tone': brand.tone,
  } as CSSProperties
  const brandImage = `linear-gradient(120deg, rgba(248, 251, 255, .82), rgba(229, 238, 247, .58)), url("${brand.logo}")`
  const revealImage = `linear-gradient(120deg, rgba(248, 251, 255, .28), rgba(229, 238, 247, .12)), url("${brand.logo}")`

  function handlePointerMove(nextPointer: PointerEvent<HTMLElement>) {
    const bounds = nextPointer.currentTarget.getBoundingClientRect()
    const x = nextPointer.clientX - bounds.left
    const y = nextPointer.clientY - bounds.top
    const edgeDistance = Math.min(x, y, bounds.width - x, bounds.height - y)
    const edgeFocus = Math.min(1, Math.max(0, (132 - edgeDistance) / 132))

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
      <div className="news-info-card-reveal" style={{ backgroundImage: revealImage }} aria-hidden="true" />
      <div className="news-info-card-shade" aria-hidden="true" />
      <div className="news-info-card-content">
        <div className="news-info-card-kicker">
          <p className="eyebrow">News Information</p>
          <span>Move toward the edge to inspect the signal</span>
        </div>
        <h2>{event.headline}</h2>
        <div className="info-row">
          <span>Source</span><b>{event.source}</b>
          <span>Published</span><b>{dateTime(event.publishedAt)}</b>
          <span>Sentiment</span><SentimentBadge sentiment={event.sentiment} score={event.sentimentScore} confidence={event.confidence} />
          <span>Score</span><b>{event.sentimentScore === null ? 'Pending' : event.sentimentScore.toFixed(2)}</b>
          <span>Confidence</span><b>{event.confidence === null ? 'Pending' : `${Math.round(event.confidence * 100)}%`}</b>
          <span>Original</span><a className="inline-link" href={event.url} target="_blank" rel="noreferrer">Open article</a>
        </div>
      </div>
    </Card>
  )
}
