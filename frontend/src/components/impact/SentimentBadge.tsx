import type { SentimentState } from '../../types/domain'

const labels: Record<Exclude<SentimentState, null>, string> = {
  POSITIVE: 'Positive',
  NEGATIVE: 'Negative',
  NEUTRAL: 'Neutral',
}

export function SentimentBadge({ sentiment, score, confidence }: { sentiment: SentimentState; score?: number | null; confidence?: number | null }) {
  if (sentiment === null) {
    return <span className="sentiment sentiment-pending" title="Analysis in progress">Analyzing</span>
  }

  const opacity = confidence === undefined || confidence === null ? 1 : Math.min(1, 0.45 + confidence * 0.55)
  const label = labels[sentiment]
  const scoreText = score === undefined || score === null ? '' : ` ${score > 0 ? '+' : ''}${score.toFixed(2)}`

  return (
    <span
      className={`sentiment sentiment-${sentiment.toLowerCase()}`}
      title={confidence === undefined || confidence === null ? undefined : `Confidence ${(confidence * 100).toFixed(0)}%`}
      style={{ opacity }}
    >
      {label}
      {scoreText}
    </span>
  )
}
