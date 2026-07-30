import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useTheme } from '../hooks/useTheme'

export function AppLayout() {
  const { theme, toggleTheme } = useTheme()
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const location = useLocation()
  const isNewsDetail = location.pathname.startsWith('/impact/')
  const isForestPreview = location.pathname === '/theme-preview/forest'

  if (isNewsDetail) {
    return (
      <main className="main news-detail-main">
        <Outlet />
      </main>
    )
  }

  return (
    <div className={`app-frame${isForestPreview ? ' forest-theme-preview' : ''}`}>
      <div className={sidebarCollapsed ? 'shell shell-collapsed' : 'shell'}>
      <aside className={sidebarCollapsed ? 'sidebar sidebar-collapsed' : 'sidebar'}>
        <div>
          <div className="brand brand-with-toggle">
            <span className="brand-mark brand-bolt-mark" role="img" aria-label="FNPIS">
              <svg viewBox="0 0 32 32" aria-hidden="true">
                <defs>
                  <linearGradient id="brand-bolt-blue" x1="5" y1="4" x2="27" y2="28" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#76c5f3" />
                    <stop offset="0.48" stopColor="#2878c8" />
                    <stop offset="1" stopColor="#164d9b" />
                  </linearGradient>
                </defs>
                <path d="M17.5 2 4 17.2h8.8l-1.2 12.8L28 13.9h-9.1L17.5 2Z" fill="url(#brand-bolt-blue)" />
              </svg>
            </span>
            <div className={sidebarCollapsed ? 'brand-copy hidden' : 'brand-copy'}>
              <strong>FNPI</strong>
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
    </div>
  )
}
