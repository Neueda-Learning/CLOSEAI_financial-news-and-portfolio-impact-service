import { NavLink, Outlet } from 'react-router-dom'
import { useTheme } from '../hooks/useTheme'

export function AppLayout() {
  const { theme, toggleTheme } = useTheme()

  return (
    <div className="shell">
      <aside className="sidebar">
        <div>
          <div className="brand">
            <span className="brand-mark">F</span>
            <div>
              <strong>FNPIS</strong>
              <p>Financial news, portfolio impact</p>
            </div>
          </div>
          <nav className="nav">
            <NavLink to="/dashboard"><span>▦</span> Dashboard</NavLink>
            <NavLink to="/portfolio-impact"><span>⇄</span> Portfolio Impact</NavLink>
          </nav>
        </div>
        <button className="theme-toggle" onClick={toggleTheme}>
          <span>◐</span> {theme === 'light' ? 'Light Mode' : 'Dark Mode'}
        </button>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
