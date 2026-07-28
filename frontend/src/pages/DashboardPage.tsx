import { Card } from '../components/common/Card'
import { LineChart } from '../components/charts/LineChart'
import { PieChart } from '../components/charts/PieChart'
import { impactEventsMock } from '../mock/impactMock'
import { portfolioSummaryMock, portfolioValueTrend } from '../mock/portfolioMock'
import { currency, percent } from '../utils/formatters'

export function DashboardPage() {
  const analyzedEvents = impactEventsMock.filter((event) => event.sentiment !== null)
  const confirmedCount = analyzedEvents.filter((event) => event.alignment === 'CONFIRMED').length
  const divergentCount = analyzedEvents.filter((event) => event.alignment === 'DIVERGENT').length
  const inconclusiveCount = analyzedEvents.filter((event) => event.alignment === 'INCONCLUSIVE').length
  const sampleSize = analyzedEvents.length
  const minimumSampleSize = 20
  const directionAgreementRate = sampleSize >= minimumSampleSize ? (confirmedCount / sampleSize) * 100 : null
  const sentimentScores = analyzedEvents.map((event) => event.sentimentScore ?? 0)
  const weightedSentiment = sentimentScores.length > 0 ? sentimentScores.reduce((sum, value) => sum + value, 0) / sentimentScores.length : 0
  const newsCoverageRate = impactEventsMock.length === 0 ? 0 : (analyzedEvents.length / impactEventsMock.length) * 100

  return (
    <div className="page-stack">
      <header className="hero">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>Morning view of portfolio health</h1>
          <p className="lede">A clean, financial-first summary of value, allocation, and sentiment trend.</p>
        </div>
        <Card className="hero-metric">
          <small>Total value</small>
          <strong>{currency(portfolioSummaryMock.totalValue)}</strong>
          <span className={portfolioSummaryMock.todayChange >= 0 ? 'positive' : 'negative'}>
            {percent(portfolioSummaryMock.todayChangePct)} today
          </span>
        </Card>
      </header>

      <section className="signal-row" aria-label="Portfolio summary">
        <Card className="signal-card">
          <small>Today's Change</small>
          <strong className="negative">{currency(portfolioSummaryMock.todayChange)}</strong>
          <span>{percent(portfolioSummaryMock.todayChangePct)}</span>
        </Card>
        <Card className="signal-card">
          <small>Tracked Symbols</small>
          <strong>{portfolioSummaryMock.allocation.length - 1}</strong>
          <span>Active holdings</span>
        </Card>
        <Card className="signal-card">
          <small>Data posture</small>
          <strong>Fresh</strong>
          <span>Mock service ready</span>
        </Card>
      </section>

      <Card className="impact-summary-overview">
        <div className="impact-summary-main">
          <small>Direction agreement</small>
          <strong>{directionAgreementRate === null ? 'Sample insufficient' : `${Math.round(directionAgreementRate)}%`}</strong>
          <div className="agreement-bar" aria-hidden="true">
            <span style={{ width: `${directionAgreementRate ?? 0}%` }} />
          </div>
          <p>
            {directionAgreementRate === null
              ? `Current ${sampleSize} analyzed records; need at least ${minimumSampleSize} before showing a rate.`
              : `Based on ${sampleSize} analyzed records.`}
          </p>
        </div>
        <div className="impact-summary-counts" aria-label="Impact assessment counts">
          <span className="alignment-confirmed">✓ Confirmed {confirmedCount}</span>
          <span className="alignment-divergent">✕ Divergent {divergentCount}</span>
          <span className="alignment-inconclusive">— Inconclusive {inconclusiveCount}</span>
        </div>
        <div className="impact-summary-metrics">
          <div>
            <small>Weighted sentiment</small>
            <strong className={weightedSentiment >= 0 ? 'positive' : 'negative'}>{weightedSentiment >= 0 ? '+' : ''}{weightedSentiment.toFixed(2)}</strong>
          </div>
          <div>
            <small>News coverage</small>
            <strong>{Math.round(newsCoverageRate)}%</strong>
          </div>
          <div>
            <small>Data as of</small>
            <strong>15:42:10</strong>
          </div>
        </div>
      </Card>

      <section className="grid-2">
        <Card>
          <h2>Portfolio Allocation</h2>
          <div className="chart-box">
            <PieChart labels={portfolioSummaryMock.allocation.map((item) => item.ticker)} values={portfolioSummaryMock.allocation.map((item) => item.weight)} />
          </div>
        </Card>
        <Card>
          <h2>Portfolio Value Trend</h2>
          <div className="chart-box">
            <LineChart
              labels={portfolioValueTrend.map((item) => item.date)}
              datasets={[
                { label: 'Total value', data: portfolioValueTrend.map((item) => item.totalValue), borderColor: '#27506f', backgroundColor: 'rgba(39, 80, 111, 0.12)' },
              ]}
            />
          </div>
        </Card>
      </section>

      <Card>
        <h2>Sentiment Overview</h2>
        <div className="chart-box">
          <LineChart
            labels={portfolioSummaryMock.sentimentTrend.map((item) => item.date)}
            datasets={[
              { label: 'Positive', data: portfolioSummaryMock.sentimentTrend.map((item) => item.positive), borderColor: '#2f7d63', backgroundColor: 'rgba(47, 125, 99, 0.18)' },
              { label: 'Negative', data: portfolioSummaryMock.sentimentTrend.map((item) => item.negative), borderColor: '#b91c1c', backgroundColor: 'rgba(185, 28, 28, 0.12)' },
            ]}
          />
        </div>
      </Card>

      <Card>
        <h2>External API Surface</h2>
        <div className="api-grid">
          <div>
            <small>Read-only access</small>
            <strong>/api/portfolios /api/holdings /api/news /api/impacts</strong>
          </div>
          <div>
            <small>Auth</small>
            <strong>API key header</strong>
          </div>
          <div>
            <small>Docs</small>
            <strong>Swagger endpoint for demos</strong>
          </div>
        </div>
      </Card>

      <Card>
        <h2>Holdings Summary</h2>
        <div className="table-wrap compact">
          <table>
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Weight</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody>
              {portfolioSummaryMock.allocation.map((item) => (
                <tr key={item.ticker}>
                  <td className="ticker">{item.ticker}</td>
                  <td>{item.weight.toFixed(1)}%</td>
                  <td>{currency(item.value)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
