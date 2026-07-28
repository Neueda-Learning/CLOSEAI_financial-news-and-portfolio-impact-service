import { Card } from '../components/common/Card'
import { LineChart } from '../components/charts/LineChart'
import { PieChart } from '../components/charts/PieChart'
import { portfolioSummaryMock } from '../mock/portfolioMock'
import { currency, percent } from '../utils/formatters'

export function DashboardPage() {
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

      <section className="grid-2">
        <Card>
          <h2>Portfolio Allocation</h2>
          <div className="chart-box">
            <PieChart labels={portfolioSummaryMock.allocation.map((item) => item.ticker)} values={portfolioSummaryMock.allocation.map((item) => item.weight)} />
          </div>
        </Card>
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
      </section>

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
