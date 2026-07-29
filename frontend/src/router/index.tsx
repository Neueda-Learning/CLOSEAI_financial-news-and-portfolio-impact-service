import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../layouts/AppLayout'
import { DashboardPage } from '../pages/DashboardPage'
import { ImpactDetailPage } from '../pages/ImpactDetailPage'
import { PortfolioImpactPage } from '../pages/PortfolioImpactPage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <Navigate to="/portfolio-impact" replace /> },
      { path: '/dashboard', element: <DashboardPage /> },
      { path: '/portfolio-impact', element: <PortfolioImpactPage /> },
      { path: '/portfolio', element: <Navigate to="/portfolio-impact" replace /> },
      { path: '/impact', element: <PortfolioImpactPage /> },
      { path: '/impact/:id', element: <ImpactDetailPage /> },
    ],
  },
])
