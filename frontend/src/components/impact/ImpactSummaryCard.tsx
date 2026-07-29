import { currency, percent } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'

export function ImpactSummaryCard({ event }: { event: ImpactEvent }) {
  const alignmentLabels: Record<ImpactEvent['alignment'], string> = {
    CONFIRMED: 'Confirmed',
    DIVERGENT: 'Divergent',
    INCONCLUSIVE: 'Inconclusive',
  }
  const directionLabels: Record<ImpactEvent['impactDirection'], string> = {
    BULLISH: 'Bullish',
    BEARISH: 'Bearish',
    NEUTRAL: 'Neutral',
  }

  return (
    <Card className="summary-strip impact-summary-strip">
      <div>
        <small>Affected holdings</small>
        <strong>{event.affectedTickers.join(', ')}</strong>
      </div>
      <div>
        <small>Impact direction</small>
        <strong>{directionLabels[event.impactDirection]}</strong>
      </div>
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
