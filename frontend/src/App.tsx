import { RouterProvider } from 'react-router-dom'
import { PortfolioProvider } from './contexts/PortfolioContext'
import { ThemeProvider } from './contexts/ThemeContext'
import { router } from './router'

function App() {
  return (
    <ThemeProvider>
      <PortfolioProvider>
        <RouterProvider router={router} />
      </PortfolioProvider>
    </ThemeProvider>
  )
}

export default App
