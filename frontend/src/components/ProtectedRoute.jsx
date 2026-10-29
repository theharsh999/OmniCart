import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Spinner } from './ui'
import { homePath } from '../utils/format'

/** Use without `roles` to require login, or with roles={['ADMIN']} for a role guard. (The backend enforces this too.) */
export default function ProtectedRoute({ roles }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner full />
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homePath(user.role)} replace />
  return <Outlet />
}
