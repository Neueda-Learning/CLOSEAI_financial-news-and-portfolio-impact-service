import { ArcElement, Chart as ChartJS, Legend, Tooltip } from 'chart.js'
import { Doughnut } from 'react-chartjs-2'

ChartJS.register(ArcElement, Tooltip, Legend)

export function PieChart({ labels, values }: { labels: string[]; values: number[] }) {
  return (
    <Doughnut
      data={{
        labels,
        datasets: [
          {
            data: values,
            backgroundColor: ['#1f4e79', '#2f7d63', '#b7791f', '#6b7280', '#d8b45f'],
            borderColor: 'rgba(255,255,255,.95)',
            borderWidth: 3,
          },
        ],
      }}
      options={{ plugins: { legend: { position: 'bottom' } }, cutout: '62%' }}
    />
  )
}
