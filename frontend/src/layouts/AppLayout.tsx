import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTheme } from '../hooks/useTheme'

export function AppLayout() {
  const { theme, toggleTheme } = useTheme()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const location = useLocation()
  const isNewsDetail = location.pathname.startsWith('/impact/')

  if (isNewsDetail) {
    return (
      <main className="main news-detail-main">
        <Outlet />
      </main>
    )
  }

  return (
    <div className={sidebarCollapsed ? 'shell shell-collapsed' : 'shell'}>
      <aside className={sidebarCollapsed ? 'sidebar sidebar-collapsed' : 'sidebar'}>
        <div>
          <div className="brand brand-with-toggle">
            <span className="brand-mark">F</span>
            <div className={sidebarCollapsed ? 'brand-copy hidden' : 'brand-copy'}>
              <strong>FNPIS</strong>
              <p>Financial news, portfolio impact</p>
            </div>
            <button
              type="button"
              className="sidebar-toggle"
              aria-label={sidebarCollapsed ? '展开左侧边栏' : '收起左侧边栏'}
              aria-pressed={sidebarCollapsed}
              onClick={() => setSidebarCollapsed((value) => !value)}
            >
              {sidebarCollapsed ? '›' : '‹'}
            </button>
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
