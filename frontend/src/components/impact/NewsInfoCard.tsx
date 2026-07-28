import { dateTime } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'
import { SentimentBadge } from './SentimentBadge'

export function NewsInfoCard({ event }: { event: ImpactEvent }) {
  return (
    <Card>
      <p className="eyebrow">News Information</p>
      <h2>{event.headline}</h2>
      <div className="info-row">
        <span>Source</span><b>{event.source}</b>
        <span>Published</span><b>{dateTime(event.publishedAt)}</b>
        <span>Sentiment</span><SentimentBadge sentiment={event.sentiment} />
        <span>Confidence</span><b>{Math.round(event.confidence * 100)}%</b>
      </div>
    </Card>
  )
}
