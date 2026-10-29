import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Store } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { Button, Field } from '../components/ui'
import { errorMessage, fieldErrors, homePath } from '../utils/format'

export default function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePath(user.role)} replace />
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!form.name.trim()) errs.name = 'Full name is required'
    if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Enter a valid email address'
    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(form.password)) errs.password = 'At least 8 characters with a letter and a number'
    if (form.confirm !== form.password) errs.confirm = 'Passwords do not match'
    setErrors(errs)
    if (Object.keys(errs).length) return
    setBusy(true); setApiError('')
    try {
      const u = await register(form.name, form.email, form.password)   // always creates a STAFF account
      navigate(homePath(u.role), { replace: true })
    } catch (err) {
      setErrors(fieldErrors(err)); setApiError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <Link to="/" className="logo" style={{ marginBottom: '1rem' }}><Store size={22} color="#4f46e5" /> OmniCart</Link>
        <h1>Create your account</h1>
        <p className="muted">New accounts are created with Cashier access.</p>
        {apiError && <div className="alert-error" role="alert">{apiError}</div>}
        <form onSubmit={submit} noValidate>
          <Field label="Full name" error={errors.name}><input className="input" value={form.name} onChange={set('name')} autoComplete="name" /></Field>
          <Field label="Email" error={errors.email}><input className="input" type="email" value={form.email} onChange={set('email')} autoComplete="email" /></Field>
          <Field label="Password" error={errors.password}><input className="input" type="password" value={form.password} onChange={set('password')} autoComplete="new-password" /></Field>
          <Field label="Confirm password" error={errors.confirm}><input className="input" type="password" value={form.confirm} onChange={set('confirm')} autoComplete="new-password" /></Field>
          <Button type="submit" loading={busy} style={{ width: '100%' }}>Create account</Button>
        </form>
        <p className="muted" style={{ marginTop: '1rem', marginBottom: 0 }}>Already registered? <Link to="/login">Log in</Link></p>
      </div>
    </div>
  )
}
