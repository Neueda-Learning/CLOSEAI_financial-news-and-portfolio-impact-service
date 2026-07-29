import { Navigate, createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../layouts/AppLayout'
import { DashboardPage } from '../pages/DashboardPage'
import { ForestThemePreviewPage } from '../pages/ForestThemePreviewPage'
import { ImpactDetailPage } from '../pages/ImpactDetailPage'
import { PortfolioImpactPage } from '../pages/PortfolioImpactPage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <Navigate to="/dashboard" replace /> },
      { path: '/dashboard', element: <DashboardPage /> },
      { path: '/theme-preview/forest', element: <ForestThemePreviewPage /> },
      { path: '/portfolio-impact', element: <PortfolioImpactPage /> },
      { path: '/portfolio', element: <Navigate to="/portfolio-impact" replace /> },
      { path: '/impact', element: <PortfolioImpactPage /> },
      { path: '/impact/:id', element: <ImpactDetailPage /> },
    ],
  },
])
