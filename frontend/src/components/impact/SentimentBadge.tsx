import type { Sentiment } from '../../types/domain'

const labels: Record<Sentiment, string> = {
  POSITIVE: 'Positive',
  NEGATIVE: 'Negative',
  NEUTRAL: 'Neutral',
}

export function SentimentBadge({ sentiment }: { sentiment: Sentiment }) {
  return <span className={`sentiment sentiment-${sentiment.toLowerCase()}`}>{labels[sentiment]}</span>
}
