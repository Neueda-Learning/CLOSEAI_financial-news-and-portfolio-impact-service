import { useEffect, useRef, useState } from 'react'
import { Button } from '../common/Button'
import { Modal } from '../common/Modal'
import { apiFetch } from '../../services/apiClient'

type HoldingFormInput = {
  ticker: string
  shares: number
  averageCost: number
}

type SecurityOption = { symbol: string; companyName: string }

export function AddHoldingModal({
  onClose,
  onSubmit,
  initialValue,
  mode = 'add',
  inline = false,
}: {
  onClose: () => void
  onSubmit: (input: HoldingFormInput) => Promise<void>
  initialValue?: Partial<HoldingFormInput>
  mode?: 'add' | 'edit'
  inline?: boolean
}) {
  const [ticker, setTicker] = useState(initialValue?.ticker ?? '')
  const [shares, setShares] = useState(initialValue?.shares !== undefined ? String(initialValue.shares) : '')
  const [averageCost, setAverageCost] = useState(initialValue?.averageCost !== undefined ? String(initialValue.averageCost) : '')
  const [results, setResults] = useState<SecurityOption[]>([])
  const [open, setOpen] = useState(false)
  const pickerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function dismiss(e: MouseEvent) { if (pickerRef.current && !pickerRef.current.contains(e.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', dismiss)
    return () => document.removeEventListener('mousedown', dismiss)
  }, [])

  useEffect(() => {
    if (mode === 'edit' || ticker.length < 1) { setResults([]); setOpen(false); return }
    const timer = setTimeout(async () => {
      try {
        const r = await apiFetch<SecurityOption[]>(`/securities?q=${encodeURIComponent(ticker)}`)
        setResults(r); setOpen(r.length > 0)
      } catch { setResults([]); setOpen(false) }
    }, 200)
    return () => clearTimeout(timer)
  }, [ticker, mode])

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    await onSubmit({ ticker, shares: Number(shares), averageCost: Number(averageCost) })
    onClose()
  }

  const form = (
    <form className="form-grid" onSubmit={submit}>
        <label>
          Ticker
          <div className="ticker-search" ref={pickerRef}>
            <input value={ticker} onChange={(e) => setTicker(e.target.value.toUpperCase())} onFocus={() => { if (results.length > 0) setOpen(true) }} placeholder="Search symbol…" required disabled={mode === 'edit'} autoComplete="off" />
            {open && (
              <div className="ticker-search-dropdown">
                {results.map((r) => (
                  <button type="button" key={r.symbol} onClick={() => { setTicker(r.symbol); setOpen(false) }}>
                    <strong>{r.symbol}</strong>
                    <span>{r.companyName}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </label>
        <label>
          Shares
          <input value={shares} onChange={(event) => setShares(event.target.value)} type="number" min="1" step="1" required />
        </label>
        <label>
          Average Cost
          <input value={averageCost} onChange={(event) => setAverageCost(event.target.value)} type="number" min="0" step="0.0001" required />
        </label>
        <Button type="submit">{mode === 'edit' ? 'Update Holding' : 'Submit'}</Button>
    </form>
  )

  if (inline) {
    return <section className="inline-holding-form" aria-label="Add holding">{form}</section>
  }

  return <Modal title={mode === 'edit' ? 'Edit Holding' : 'Add Holding'} onClose={onClose}>{form}</Modal>
}
