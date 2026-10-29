const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })
export const formatMoney = (n) => money.format(Number(n || 0))
export const formatDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '—'

/** Turns an Axios error into a readable message (and field errors, if any). */
export const errorMessage = (err) =>
  err?.response?.data?.message || (err?.request ? 'Cannot reach the server. Is the backend running?' : 'Something went wrong')
export const fieldErrors = (err) => err?.response?.data?.fieldErrors || {}
export const homePath = (role) => (role === 'ADMIN' ? '/admin/dashboard' : '/staff/dashboard')
