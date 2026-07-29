import { currency, percent } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'

export function ImpactSummaryCard({ event }: { event: ImpactEvent }) {
  const alignmentLabels: Record<NonNullable<ImpactEvent['alignment']>, string> = {
    CONFIRMED: '✓ Confirmed',
    DIVERGENT: '✕ Divergent',
    INCONCLUSIVE: '— Inconclusive',
  }

  if (event.sentiment === null || event.alignment === null) {
    return (
      <Card className="summary-strip impact-pending-strip">
        <div>
          <small>Impact status</small>
          <strong>Analysis in progress</strong>
        </div>
        <div>
          <small>Reason</small>
          <strong>Sentiment result is not ready yet</strong>
        </div>
        <div>
          <small>Portfolio impact</small>
          <strong>Pending</strong>
        </div>
        <div>
          <small>Next step</small>
          <strong>Refresh after agent completes</strong>
        </div>
      </Card>
    )
  }

  return (
    <Card className="summary-strip">
      <div>
        <small>Impact strength</small>
        <strong>{event.strength}</strong>
      </div>
      <div>
        <small>Alignment</small>
        <strong className={`alignment alignment-${event.alignment.toLowerCase()}`}>{alignmentLabels[event.alignment]}</strong>
      </div>
      <div>
        <small>Price change</small>
        <strong className={event.priceChange >= 0 ? 'positive' : 'negative'}>{percent(event.priceChange)}</strong>
      </div>
      <div>
        <small>Portfolio impact</small>
        <strong className={event.portfolioImpact >= 0 ? 'positive' : 'negative'}>{currency(event.portfolioImpact)}</strong>
      </div>
    </Card>
  )
}
