import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { Card } from '../components/common/Card'
import { LineChart } from '../components/charts/LineChart'
import { ImpactSummaryCard } from '../components/impact/ImpactSummaryCard'
import { NewsInfoCard } from '../components/impact/NewsInfoCard'
import { impactService } from '../services/impactService'
import type { ImpactEvent } from '../types/domain'
import { usePortfolio } from '../hooks/usePortfolio'

export function ImpactDetailPage() {
  const { id } = useParams()
  const [event, setEvent] = useState<ImpactEvent | undefined>(undefined)
  const { activePortfolioId } = usePortfolio()

  useEffect(() => {
    const eventId = Number(id)
    if (eventId && activePortfolioId) impactService.getImpactEvent(eventId, activePortfolioId).then(setEvent)
  }, [id, activePortfolioId])

  if (!event) {
    return <p>Loading...</p>
  }

  return (
    <div className="page-stack">
      <p className="eyebrow">Impact detail</p>
      <NewsInfoCard event={event} />
      <section className="grid-2">
        <Card>
          <h2>News Content</h2>
          <p className="body-copy">{event.content}</p>
        </Card>
        <Card>
          <h2>Stock Price Chart</h2>
          <div className="chart-box">
            <LineChart
              labels={event.priceSeries.map((point) => point.time)}
              datasets={[
                {
                  label: event.affectedTickers.join(', '),
                  data: event.priceSeries.map((point) => point.price),
                  borderColor: '#1f4e79',
                  backgroundColor: 'rgba(31, 78, 121, 0.15)',
                },
              ]}
              markerLabel={event.priceSeries[2]?.time}
            />
          </div>
        </Card>
      </section>
      <ImpactSummaryCard event={event} />
    </div>
  )
}
