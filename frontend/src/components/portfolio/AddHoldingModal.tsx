import { useState } from 'react'
import { Button } from '../common/Button'
import { Modal } from '../common/Modal'

export function AddHoldingModal({
  onClose,
  onSubmit,
}: {
  onClose: () => void
  onSubmit: (input: { ticker: string; shares: number; averageCost: number }) => Promise<void>
}) {
  const [ticker, setTicker] = useState('')
  const [shares, setShares] = useState('')
  const [averageCost, setAverageCost] = useState('')

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    await onSubmit({ ticker, shares: Number(shares), averageCost: Number(averageCost) })
    onClose()
  }

  return (
    <Modal title="Add Holding" onClose={onClose}>
      <form className="form-grid" onSubmit={submit}>
        <label>
          Ticker
          <input value={ticker} onChange={(event) => setTicker(event.target.value)} placeholder="AAPL" required />
        </label>
        <label>
          Shares
          <input value={shares} onChange={(event) => setShares(event.target.value)} type="number" min="1" required />
        </label>
        <label>
          Average Cost
          <input value={averageCost} onChange={(event) => setAverageCost(event.target.value)} type="number" min="0" required />
        </label>
        <Button type="submit">Submit</Button>
      </form>
    </Modal>
  )
}
