import { Link } from 'react-router-dom'
import { currency, dateTime, percent } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'
import { SentimentBadge } from './SentimentBadge'

export function ImpactCard({ event }: { event: ImpactEvent }) {
  return (
    <Card className="impact-card">
      <div>
        <p className="eyebrow">{event.affectedTickers.join(', ')} - {dateTime(event.publishedAt)}</p>
        <h3>{event.headline}</h3>
      </div>
      <div className="impact-metrics">
        <span><small>Sentiment</small><SentimentBadge sentiment={event.sentiment} score={event.sentimentScore} confidence={event.confidence} /></span>
        <span><small>Confidence</small>{Math.round(event.confidence * 100)}%</span>
        <span><small>Direction</small>{event.impactDirection}</span>
        <span><small>Price Change</small><b className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)}</b></span>
        <span><small>Portfolio Impact</small><b className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)}</b></span>
      </div>
      <Link className="button-link primary-link" to={`/impact/${event.id}`}>
        View Analysis
      </Link>
    </Card>
  )
}
