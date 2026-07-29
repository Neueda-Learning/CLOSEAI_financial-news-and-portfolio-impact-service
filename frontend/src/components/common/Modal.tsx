import type { ReactNode } from 'react'
import { Button } from './Button'

export function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="modal-backdrop" role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-header">
          <h2 id="modal-title">{title}</h2>
          <Button variant="ghost" onClick={onClose} aria-label="Close modal">
            ×
          </Button>
        </div>
        {children}
      </div>
    </div>
  )
}
