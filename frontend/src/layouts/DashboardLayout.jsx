import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Package, Tags, Users, Boxes, Receipt, History, LogOut, Store, UserCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

export default function DashboardLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const isAdmin = user.role === 'ADMIN'
  const links = [
    { to: isAdmin ? '/admin/dashboard' : '/staff/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    ...(isAdmin ? [
      { to: '/admin/products', label: 'Products', icon: Package },
      { to: '/admin/categories', label: 'Categories', icon: Tags },
      { to: '/admin/cashiers', label: 'Cashier Management', icon: Users },
    ] : []),
    { to: '/inventory', label: 'Inventory', icon: Boxes },
    { to: '/billing', label: 'Billing', icon: Receipt },
    { to: '/sales', label: 'Sales History', icon: History },
    { to: '/profile', label: 'My Profile', icon: UserCircle },
  ]

  const initials = user?.name ? user.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase() : 'U'
  const displayRole = user?.displayRole || (isAdmin ? 'Owner' : 'Cashier')

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><Store size={22} /> OmniCart</div>
        <nav>
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-chip">
            {user?.avatar ? (
              <img src={user.avatar} alt={user.name} className="user-avatar-img" />
            ) : (
              <div className="user-avatar-circle">{initials}</div>
            )}
            <div className="user-info-text">
              <strong>{user?.name}</strong>
              <div className="user-role-line">
                <span className="role-tag">{displayRole}</span>
                {user?.role !== 'ADMIN' && user?.employeeId && <span className="empid-tag">{user.employeeId}</span>}
              </div>
            </div>
          </div>
          <button className="nav-link" onClick={() => { logout(); navigate('/') }}><LogOut size={18} /> Log out</button>
        </div>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  )
}

