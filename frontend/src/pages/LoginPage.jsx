import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Store, ShieldCheck, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { Button, Field } from '../components/ui'
import { errorMessage, homePath } from '../utils/format'

const DEMOS = {
  owner: { email: 'admin@omnicart.local', password: 'Admin@123' },
  omkar: { email: 'omkar@gmail.com', password: '1234' },
  ansh: { email: 'ansh@gmail.com', password: '1234' },
}

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [show, setShow] = useState(false)
  const [busy, setBusy] = useState('')   // '', 'form', 'owner', 'omkar', 'ansh'
  const [apiError, setApiError] = useState('')

  if (user) return <Navigate to={homePath(user.role)} replace />

  const signIn = async (email, password, which) => {
    setBusy(which); setApiError('')
    try {
      const u = await login(email, password)   // real call to POST /api/auth/login
      navigate(homePath(u.role), { replace: true })
    } catch (e) {
      setApiError(errorMessage(e))
    } finally {
      setBusy('')
    }
  }

  const submit = (e) => {
    e.preventDefault()
    const errs = {}
    if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Enter a valid email address'
    if (!form.password) errs.password = 'Password is required'
    setErrors(errs)
    if (!Object.keys(errs).length) signIn(form.email, form.password, 'form')
  }

  return (
    <div className="auth-page">
      <div className="card auth-card">
        <Link to="/" className="logo" style={{ marginBottom: '1rem' }}><Store size={22} color="#4f46e5" /> OmniCart</Link>
        <h1>Log in</h1>
        <p className="muted">Welcome back. Enter your details to continue.</p>
        {apiError && <div className="alert-error" role="alert">{apiError}</div>}
        <form onSubmit={submit} noValidate>
          <Field label="Email" error={errors.email}>
            <input className="input" type="email" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
          <Field label="Password" error={errors.password}>
            <div className="input-wrap">
              <input className="input" type={show ? 'text' : 'password'} autoComplete="current-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
              <button type="button" className="icon-btn" onClick={() => setShow(!show)} aria-label={show ? 'Hide password' : 'Show password'}>{show ? <EyeOff size={18} /> : <Eye size={18} />}</button>
            </div>
          </Field>
          <Button type="submit" loading={busy === 'form'} style={{ width: '100%' }}>Log in</Button>
        </form>

        <div className="demo-box" style={{ marginTop: '1.25rem' }}>
          <Button
            variant="secondary"
            loading={busy === 'owner'}
            onClick={() => signIn(DEMOS.owner.email, DEMOS.owner.password, 'owner')}
            style={{ width: '100%', marginBottom: '0.85rem' }}
          >
            <ShieldCheck size={16} /> Login as Owner
          </Button>

          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '0.75rem', width: '100%' }}>
            <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem', textAlign: 'center' }}>
              Demo Cashier Login
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              <Button variant="ghost" loading={busy === 'omkar'} onClick={() => signIn(DEMOS.omkar.email, DEMOS.omkar.password, 'omkar')}>
                <UserRound size={15} /> Omkar
              </Button>
              <Button variant="ghost" loading={busy === 'ansh'} onClick={() => signIn(DEMOS.ansh.email, DEMOS.ansh.password, 'ansh')}>
                <UserRound size={15} /> Ansh
              </Button>
            </div>
            <div style={{ textAlign: 'center', marginTop: '0.45rem', fontSize: '0.75rem', color: 'var(--muted)' }}>
              Local demonstration accounts (EMP-001 &amp; EMP-002)
            </div>
          </div>
        </div>

        <p className="muted" style={{ marginTop: '1rem', marginBottom: 0 }}>New here? <Link to="/register">Create a cashier account</Link></p>
      </div>
    </div>
  )
}
