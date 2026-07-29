import { Button } from '../common/Button'
import { currency, percent } from '../../utils/formatters'
import type { Holding } from '../../types/domain'

export function HoldingTable({ holdings, onDelete }: { holdings: Holding[]; onDelete: (id: number) => void }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Ticker</th>
            <th>Company</th>
            <th>Shares</th>
            <th>Average cost</th>
            <th>Current price</th>
            <th>Day</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((holding) => (
            <tr key={holding.id}>
              <td className="ticker">{holding.ticker}</td>
              <td>{holding.companyName}</td>
              <td>{holding.shares}</td>
              <td>{currency(holding.averageCost)}</td>
              <td>{currency(holding.currentPrice)}</td>
              <td className={holding.dayChangePct >= 0 ? 'positive' : 'negative'}>{percent(holding.dayChangePct)}</td>
              <td>
                <Button variant="danger" onClick={() => onDelete(holding.id)}>
                  Delete
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
