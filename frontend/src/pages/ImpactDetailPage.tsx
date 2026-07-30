import { useNavigate, useParams } from 'react-router-dom'
import { useEffect, useRef, useState, type PointerEvent } from 'react'
import { Card } from '../components/common/Card'
import { LineChart } from '../components/charts/LineChart'
import { ImpactSummaryCard } from '../components/impact/ImpactSummaryCard'
import { NewsInfoCard } from '../components/impact/NewsInfoCard'
import { impactService } from '../services/impactService'
import type { ImpactEvent } from '../types/domain'
import { usePortfolio } from '../hooks/usePortfolio'

export function ImpactDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [event, setEvent] = useState<ImpactEvent | undefined>(undefined)
  const [nextArticle, setNextArticle] = useState<{ id: number; headline: string; source: string } | null>(null)
  const [previousArticle, setPreviousArticle] = useState<{ id: number; headline: string; source: string } | null>(null)
  const [dragX, setDragX] = useState(0)
  const [isDragging, setIsDragging] = useState(false)
  const [isCommitting, setIsCommitting] = useState(false)
  const [commitDirection, setCommitDirection] = useState<'next' | 'previous'>('next')
  const { activePortfolioId } = usePortfolio()
  const gesture = useRef<{ pointerId: number; startX: number; lastX: number; lastTime: number; velocity: number } | null>(null)
  const commitTimer = useRef<number | null>(null)

  useEffect(() => {
    const eventId = Number(id)
    if (!eventId || !activePortfolioId) return
    let cancelled = false
    setDragX(0)
    setIsCommitting(false)
    void Promise.all([impactService.getImpactEvent(eventId, activePortfolioId), impactService.getNextNews(eventId), impactService.getPreviousNews(eventId)]).then(([nextEvent, nextNews, previousNews]) => {
      if (!cancelled) {
        setEvent(nextEvent)
        setNextArticle(nextNews)
        setPreviousArticle(previousNews)
      }
    })
    return () => { cancelled = true }
  }, [id, activePortfolioId])

  useEffect(() => () => {
    if (commitTimer.current !== null) window.clearTimeout(commitTimer.current)
  }, [])

  function isInteractiveTarget(target: EventTarget | null) {
    return target instanceof Element && Boolean(target.closest('a, button, input, select, textarea, label'))
  }

  function handlePointerDown(pointer: PointerEvent<HTMLElement>) {
    if (!nextArticle || isCommitting || isInteractiveTarget(pointer.target)) return
    pointer.currentTarget.setPointerCapture(pointer.pointerId)
    gesture.current = { pointerId: pointer.pointerId, startX: pointer.clientX, lastX: pointer.clientX, lastTime: performance.now(), velocity: 0 }
    setIsDragging(true)
  }

  function handlePointerMove(pointer: PointerEvent<HTMLElement>) {
    const currentGesture = gesture.current
    if (!currentGesture || currentGesture.pointerId !== pointer.pointerId) return
    const now = performance.now()
    const delta = Math.max(0, pointer.clientX - currentGesture.startX)
    currentGesture.velocity = (pointer.clientX - currentGesture.lastX) / Math.max(1, now - currentGesture.lastTime)
    currentGesture.lastX = pointer.clientX
    currentGesture.lastTime = now
    setDragX(delta)
  }

  function finishGesture(pointer: PointerEvent<HTMLElement>) {
    const currentGesture = gesture.current
    if (!currentGesture || currentGesture.pointerId !== pointer.pointerId) return
    gesture.current = null
    setIsDragging(false)
    const releasedX = Math.max(0, pointer.clientX - currentGesture.startX)
    const shouldAdvance = Boolean(nextArticle) && (releasedX > 92 || currentGesture.velocity > 0.6)
    if (!shouldAdvance) {
      setDragX(0)
      return
    }
    advanceToNext(releasedX)
  }

  function advanceToNext(releasedX = 0) {
    if (!nextArticle || isCommitting) return
    setCommitDirection('next')
    setIsCommitting(true)
    setDragX(Math.max(window.innerWidth, releasedX + 220))
    commitTimer.current = window.setTimeout(() => navigate(`/impact/${nextArticle.id}`), 280)
  }

  function advanceToPrevious() {
    if (!previousArticle || isCommitting) return
    setCommitDirection('previous')
    setIsCommitting(true)
    setDragX(-window.innerWidth)
    commitTimer.current = window.setTimeout(() => navigate(`/impact/${previousArticle.id}`), 280)
  }

  if (!event) {
    return <p>Loading...</p>
  }

  const swipeProgress = Math.min(Math.abs(dragX) / Math.max(1, window.innerWidth * 0.72), 1)
  const nextCardTransform = `translate3d(${(-42 + swipeProgress * 42).toFixed(1)}px, 0, 0) scale(${(0.94 + swipeProgress * 0.06).toFixed(3)})`
  const detailTransform = `translate3d(${dragX}px, 0, 0) scale(${(1 - swipeProgress * 0.035).toFixed(3)})`

  return (
    <main
      className={isDragging ? 'news-detail-swipe-shell is-dragging' : isCommitting ? `news-detail-swipe-shell is-committing is-going-${commitDirection}` : 'news-detail-swipe-shell'}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={finishGesture}
      onPointerCancel={finishGesture}
    >
      {nextArticle && <aside className="news-swipe-next-card" aria-hidden="true" style={{ transform: nextCardTransform }}><small>Up next</small><strong>{nextArticle.headline}</strong><span>{nextArticle.source} · Swipe right to open</span></aside>}
      <div className="news-detail-swipe-content" style={{ transform: detailTransform }}>
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
      </div>
      {(previousArticle || nextArticle) && <div className="news-swipe-controls" aria-label="News navigation">{previousArticle && <button type="button" className="news-swipe-next-button" onClick={advanceToPrevious}><span aria-hidden="true">←</span> Previous</button>}{nextArticle && <button type="button" className="news-swipe-next-button" onClick={() => advanceToNext()}><span aria-hidden="true">→</span> Next</button>}</div>}
    </main>
  )
}
