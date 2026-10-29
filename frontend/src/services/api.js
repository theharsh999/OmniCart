import axios from 'axios'

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' })

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('omnicart_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Expired/invalid token on a protected call -> back to login
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith('/api/auth/login') || err.config?.url?.startsWith('/api/auth/register')
    if (err.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem('omnicart_token')
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api
