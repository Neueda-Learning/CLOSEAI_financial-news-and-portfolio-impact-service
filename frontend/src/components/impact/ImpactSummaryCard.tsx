import { currency, percent } from '../../utils/formatters'
import type { ImpactEvent } from '../../types/domain'
import { Card } from '../common/Card'

export function ImpactSummaryCard({ event }: { event: ImpactEvent }) {
  return (
    <Card className="summary-strip">
      <div>
        <small>Impact strength</small>
        <strong>{event.strength}</strong>
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
