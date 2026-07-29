import {
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js'
import annotationPlugin from 'chartjs-plugin-annotation'
import { Line } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend, annotationPlugin)

type Dataset = {
  label: string
  data: number[]
  borderColor: string
  backgroundColor: string
}

export function LineChart({ labels, datasets, markerLabel }: { labels: string[]; datasets: Dataset[]; markerLabel?: string }) {
  const markerIndex = markerLabel ? labels.indexOf(markerLabel) : -1

  return (
    <Line
      data={{ labels, datasets: datasets.map((dataset) => ({ ...dataset, tension: 0.35, fill: true, pointRadius: 3 })) }}
      options={{
        maintainAspectRatio: false,
        scales: { y: { beginAtZero: false }, x: { grid: { display: false } } },
        plugins: {
          legend: { position: 'bottom' },
          annotation:
            markerIndex >= 0
              ? {
                  annotations: {
                    eventLine: {
                      type: 'line',
                      xMin: markerIndex,
                      xMax: markerIndex,
                      borderColor: '#b7791f',
                      borderWidth: 2,
                      label: { display: true, content: 'News', backgroundColor: '#fff7df', color: '#7a4b00' },
                    },
                  },
                }
              : undefined,
        },
      }}
    />
  )
}
