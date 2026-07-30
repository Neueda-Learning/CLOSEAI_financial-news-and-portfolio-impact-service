import { useRef, useState } from 'react'
import { ArcElement, Chart as ChartJS, Legend, Tooltip, type Chart } from 'chart.js'
import { Doughnut } from 'react-chartjs-2'
import { currency } from '../../utils/formatters'

const allocationColors = ['#43718e', '#8a5a3c', '#bd8d63', '#3a2e2d', '#d6b393', '#6e8e9e']

const hoverArcShadow = {
  id: 'allocationHoverShadow',
  afterDatasetDraw(chart: Chart, args: { index: number }) {
    if (args.index !== 0) return
    const active = chart.getActiveElements().find((item) => item.datasetIndex === 0)
    if (!active) return

    const arc = chart.getDatasetMeta(0).data[active.index] as ArcElement
    const context = chart.ctx
    context.save()
    context.shadowColor = 'rgba(15, 37, 62, .28)'
    context.shadowBlur = 16
    context.shadowOffsetY = 7
    arc.draw(context)
    context.restore()
  },
}

ChartJS.register(ArcElement, Tooltip, Legend, hoverArcShadow)

export function PieChart({ labels, values, colors = allocationColors, onSliceClick }: { labels: string[]; values: number[]; colors?: string[]; onSliceClick?: (label: string) => void }) {
  const chartRef = useRef<ChartJS<'doughnut'> | null>(null)
  const [activeIndex, setActiveIndex] = useState<number | null>(null)
  const total = values.reduce((sum, value) => sum + value, 0)
  const activeValue = activeIndex == null ? null : values[activeIndex]
  const activeLabel = activeIndex == null ? null : labels[activeIndex]
  const activeWeight = activeValue == null || total === 0 ? 0 : (activeValue / total) * 100

  function setFocusedSlice(index: number | null) {
    setActiveIndex(index)
    const chart = chartRef.current
    if (!chart) return
    chart.setActiveElements(index == null ? [] : [{ datasetIndex: 0, index }])
    chart.update()
  }

  return (
    <div className="allocation-chart" aria-label="Portfolio allocation chart">
      <div className="allocation-visual">
        <div className="allocation-donut">
          <Doughnut
          ref={chartRef}
          data={{
            labels,
            datasets: [{
              data: values,
              backgroundColor: labels.map((_, index) => colors[index % colors.length]),
              borderColor: 'rgba(255,255,255,.95)',
              borderWidth: 3,
              hoverOffset: 14,
            }],
          }}
          options={{
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 220, easing: 'easeOutQuart' },
            cutout: '62%',
            onHover: (_, elements) => setActiveIndex(elements[0]?.index ?? null),
            plugins: {
              legend: { display: false },
              tooltip: {
                displayColors: false,
                padding: 12,
                backgroundColor: 'rgba(14, 29, 47, .94)',
                titleFont: { weight: 600 },
                callbacks: {
                  title: (items) => `${items[0]?.label ?? ''} holding`,
                  label: (item) => `Total value  ${currency(Number(item.raw))}`,
                  afterLabel: (item) => `Portfolio weight  ${(total === 0 ? 0 : (Number(item.raw) / total) * 100).toFixed(1)}%`,
                },
              },
            },
          }}
          />
        </div>
        <div className={activeIndex == null ? 'allocation-focus' : 'allocation-focus is-active'} aria-live="polite">
          {activeIndex == null ? <span>Hover a holding slice to inspect its allocation</span> : <><small>{activeLabel}</small><strong>{currency(activeValue ?? 0)}</strong><span>{activeWeight.toFixed(1)}% of portfolio</span></>}
        </div>
      </div>
      <div className="allocation-slice-list" aria-label="Allocation holdings">
        {labels.map((label, index) => (
          <button
            type="button"
            key={label}
            className={activeIndex === index ? 'is-active' : ''}
            onMouseEnter={() => setFocusedSlice(index)}
            onMouseLeave={() => setFocusedSlice(null)}
            onFocus={() => setFocusedSlice(index)}
            onBlur={() => setFocusedSlice(null)}
            onClick={() => { setFocusedSlice(activeIndex === index ? null : index); onSliceClick?.(label) }}
          >
            <i style={{ backgroundColor: colors[index % colors.length] }} aria-hidden="true" />
            <span>{label}</span>
            <b>{total === 0 ? '0.0' : ((values[index] / total) * 100).toFixed(1)}%</b>
          </button>
        ))}
      </div>
    </div>
  )
}
