import { useState } from 'react'
import { Button } from '../common/Button'
import { Modal } from '../common/Modal'

type HoldingFormInput = {
  ticker: string
  shares: number
  averageCost: number
}

export function AddHoldingModal({
  onClose,
  onSubmit,
  initialValue,
  mode = 'add',
}: {
  onClose: () => void
  onSubmit: (input: HoldingFormInput) => Promise<void>
  initialValue?: Partial<HoldingFormInput>
  mode?: 'add' | 'edit'
}) {
  const [ticker, setTicker] = useState(initialValue?.ticker ?? '')
  const [shares, setShares] = useState(initialValue?.shares !== undefined ? String(initialValue.shares) : '')
  const [averageCost, setAverageCost] = useState(initialValue?.averageCost !== undefined ? String(initialValue.averageCost) : '')

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    await onSubmit({ ticker, shares: Number(shares), averageCost: Number(averageCost) })
    onClose()
  }

  return (
    <Modal title={mode === 'edit' ? 'Edit Holding' : 'Add Holding'} onClose={onClose}>
      <form className="form-grid" onSubmit={submit}>
        <label>
          Ticker
          <input value={ticker} onChange={(event) => setTicker(event.target.value)} placeholder="AAPL" required disabled={mode === 'edit'} />
        </label>
        <label>
          Shares
          <input value={shares} onChange={(event) => setShares(event.target.value)} type="number" min="1" required />
        </label>
        <label>
          Average Cost
          <input value={averageCost} onChange={(event) => setAverageCost(event.target.value)} type="number" min="0" required />
        </label>
        <Button type="submit">{mode === 'edit' ? 'Update Holding' : 'Submit'}</Button>
      </form>
    </Modal>
  )
}
