import { useAuth } from '../context/AuthContext'

type View = 'dashboard' | 'products' | 'scan' | 'detail' | 'create' | 'users' | 'scan-result'

interface SidebarProps {
  view: View
  onNavigate: (view: View) => void
  open: boolean
  onToggle: () => void
}

function getNavItems(role?: string): { view: View; label: string; icon: string }[] {
  const items: { view: View; label: string; icon: string }[] = []

  if (role === 'staff' || role === 'admin') {
    items.push({ view: 'products', label: 'Produk', icon: '\u{1F4CB}' })
    items.push({ view: 'scan', label: 'Pindai', icon: '\u{1F4F7}' })
  }
  if (role === 'manager' || role === 'admin') {
    items.push({ view: 'users', label: 'Manajemen Staf', icon: '\u{1F465}' })
  }

  return items
}

export default function Sidebar({ view, onNavigate, open, onToggle }: SidebarProps) {
  const { user, logout } = useAuth()

  return (
    <>
      {open && <div className="sidebar-overlay" onClick={onToggle} />}
      <aside className={`sidebar ${open ? 'sidebar-open' : ''}`}>
        <div className="sidebar-header">
          <h1 className="sidebar-title">Makmur</h1>
          <button className="sidebar-close" onClick={onToggle}>&times;</button>
        </div>

        <nav className="sidebar-nav">
          {getNavItems(user?.role).map(item => (
            <button
              key={item.view}
              className={`sidebar-nav-item ${view === item.view ? 'sidebar-nav-active' : ''}`}
              onClick={() => { onNavigate(item.view); onToggle() }}
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <span className="sidebar-user">
            {user?.username} <span className="sidebar-role">({user?.role})</span>
          </span>
          <button className="btn btn-logout sidebar-logout" onClick={logout}>Keluar</button>
        </div>
      </aside>
    </>
  )
}