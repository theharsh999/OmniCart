import { createContext, useContext, useEffect, useState } from 'react'
import api from '../services/api'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  // Synchronously restore cached user from localStorage to eliminate flicker on browser navigation
  const [user, setUser] = useState(() => {
    try {
      const cached = localStorage.getItem('omnicart_user')
      return cached ? JSON.parse(cached) : null
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(!!localStorage.getItem('omnicart_token') && !localStorage.getItem('omnicart_user'))

  // Validate session with backend on load
  useEffect(() => {
    const token = localStorage.getItem('omnicart_token')
    if (!token) {
      setLoading(false)
      return
    }
    api.get('/api/auth/me')
      .then((res) => {
        setUser(res.data)
        localStorage.setItem('omnicart_user', JSON.stringify(res.data))
      })
      .catch(() => {
        localStorage.removeItem('omnicart_token')
        localStorage.removeItem('omnicart_user')
        setUser(null)
      })
      .finally(() => setLoading(false))
  }, [])

  const handleAuth = (data) => {
    localStorage.setItem('omnicart_token', data.token)
    localStorage.setItem('omnicart_user', JSON.stringify(data.user))
    setUser(data.user)
    return data.user
  }

  const updateUser = (u) => {
    localStorage.setItem('omnicart_user', JSON.stringify(u))
    setUser(u)
  }

  const login = async (email, password) => handleAuth((await api.post('/api/auth/login', { email, password })).data)
  const register = async (name, email, password) => handleAuth((await api.post('/api/auth/register', { name, email, password })).data)
  const logout = () => {
    localStorage.removeItem('omnicart_token')
    localStorage.removeItem('omnicart_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, setUser, updateUser, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
