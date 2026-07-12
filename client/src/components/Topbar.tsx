import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

type View = 'dashboard' | 'products' | 'scan' | 'detail' | 'create' | 'users' | 'scan-result';

interface TopbarProps {
  view: View;
  onNavigate: (view: View) => void;
}

function getNavItems(role?: string): { view: View; label: string }[] {
  const items: { view: View; label: string }[] = [];

  if (role === 'staff' || role === 'admin') {
    items.push({ view: 'products', label: 'Produk' });
    items.push({ view: 'scan', label: 'Pindai' });
  }
  if (role === 'manager' || role === 'admin') {
    items.push({ view: 'users', label: 'Manajemen Staf' });
  }

  return items;
}

export default function Topbar({ view, onNavigate }: TopbarProps) {
  const { user, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navItems = getNavItems(user?.role);

  return (
    <header className="topbar">
      <div className="topbar-container">
        <div className="topbar-brand">
          <h1 className="topbar-title">Makmur</h1>
        </div>

        <nav className={`topbar-nav ${mobileMenuOpen ? 'topbar-nav-open' : ''}`}>
          {navItems.map(item => (
            <button
              key={item.view}
              className={`topbar-nav-item ${view === item.view ? 'topbar-nav-active' : ''}`}
              onClick={() => {
                onNavigate(item.view);
                setMobileMenuOpen(false);
              }}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="topbar-user">
          <span className="topbar-username">
            {user?.username} <span className="topbar-role">({user?.role})</span>
          </span>
          <button className="btn btn-logout topbar-logout" onClick={logout}>
            Keluar
          </button>
        </div>

        <button
          className="hamburger"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-label={mobileMenuOpen ? 'Tutup menu' : 'Buka menu'}
          aria-expanded={mobileMenuOpen}
        >
          <span className={`hamburger-line ${mobileMenuOpen ? 'hamburger-line-open' : ''}`}></span>
          <span className={`hamburger-line ${mobileMenuOpen ? 'hamburger-line-open' : ''}`}></span>
          <span className={`hamburger-line ${mobileMenuOpen ? 'hamburger-line-open' : ''}`}></span>
        </button>
      </div>
    </header>
  );
}