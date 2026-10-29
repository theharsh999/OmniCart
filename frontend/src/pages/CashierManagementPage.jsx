import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Users,
  Pencil,
  History,
  ShieldOff,
  ShieldCheck,
  Hash,
  Mail,
  Phone,
  Eye,
  X,
  Save,
  Lock,
  AlertTriangle,
  CheckCircle2,
  Receipt,
} from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import { Button, DataTable, EmptyState, ErrorState, Field, Modal, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { formatDate, formatMoney, errorMessage } from '../utils/format'

export default function CashierManagementPage() {
  const { data: cashiers, loading, error, reload } = useFetch(async () => (await api.get('/api/cashiers')).data)

  // Edit cashier modal state
  const [editingCashier, setEditingCashier] = useState(null)
  const [editForm, setEditForm] = useState({ name: '', phone: '' })
  const [editErrors, setEditErrors] = useState({})
  const [editSaving, setEditSaving] = useState(false)

  // Cashier sales modal state
  const [salesCashier, setSalesCashier] = useState(null)
  const [cashierSales, setCashierSales] = useState([])
  const [salesLoading, setSalesLoading] = useState(false)
  const [salesError, setSalesError] = useState('')

  // Deactivate/delete confirmation dialog state
  const [deactivatingCashier, setDeactivatingCashier] = useState(null)
  const [deactivateBusy, setDeactivateBusy] = useState(false)

  // Toast / feedback state
  const [feedback, setFeedback] = useState({ text: '', type: '' })

  const showFeedback = (text, type = 'success') => {
    setFeedback({ text, type })
    setTimeout(() => setFeedback({ text: '', type: '' }), 5000)
  }

  // Open edit modal
  const handleOpenEdit = (c) => {
    setEditingCashier(c)
    setEditForm({ name: c.name || '', phone: c.phone || '' })
    setEditErrors({})
  }

  // Save edit form
  const handleSaveEdit = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!editForm.name.trim()) errs.name = 'Full name is required'
    else if (editForm.name.trim().length > 100) errs.name = 'Name cannot exceed 100 characters'

    if (editForm.phone.trim() && !/^[0-9+\-\s()]{7,20}$/.test(editForm.phone.trim())) {
      errs.phone = 'Enter a valid phone number'
    }
    setEditErrors(errs)
    if (Object.keys(errs).length) return

    setEditSaving(true)
    try {
      await api.put(`/api/cashiers/${editingCashier.id}`, {
        name: editForm.name.trim(),
        phone: editForm.phone.trim(),
      })
      showFeedback(`Updated details for ${editingCashier.name}`)
      setEditingCashier(null)
      reload()
    } catch (err) {
      setEditErrors({ api: errorMessage(err) })
    } finally {
      setEditSaving(false)
    }
  }

  // Open cashier sales modal
  const handleViewSales = async (c) => {
    setSalesCashier(c)
    setSalesLoading(true)
    setSalesError('')
    try {
      const res = await api.get(`/api/cashiers/${c.id}/sales`)
      setCashierSales(res.data)
    } catch (err) {
      setSalesError(errorMessage(err))
    } finally {
      setSalesLoading(false)
    }
  }

  // Toggle active/inactive or safe deactivate
  const handleConfirmDeactivate = async () => {
    if (!deactivatingCashier) return
    setDeactivateBusy(true)
    try {
      const res = await api.delete(`/api/cashiers/${deactivatingCashier.id}`)
      showFeedback(res.data?.message || `Updated status for ${deactivatingCashier.name}`)
      setDeactivatingCashier(null)
      reload()
    } catch (err) {
      showFeedback(errorMessage(err), 'error')
    } finally {
      setDeactivateBusy(false)
    }
  }

  if (loading) return <Spinner />
  if (error) return <ErrorState message={error} onRetry={reload} />

  return (
    <>
      <PageHeader
        title="Cashier Management"
        subtitle="Manage cashier profiles, employee IDs, status, and view individual sales history"
      />

      {feedback.text && (
        <div
          className={feedback.type === 'error' ? 'alert-error' : 'alert-success'}
          style={{ marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          {feedback.type === 'error' ? <AlertTriangle size={18} /> : <CheckCircle2 size={18} />}
          <span>{feedback.text}</span>
        </div>
      )}

      <div className="card">
        <DataTable
          rows={cashiers}
          empty={
            <EmptyState
              title="No cashiers found"
              text="Cashier accounts seeded or registered will show up here."
            />
          }
          columns={[
            {
              header: 'Cashier',
              render: (c) => {
                const initials = c.name
                  ? c.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase()
                  : 'C'
                return (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    {c.avatar ? (
                      <img src={c.avatar} alt={c.name} className="user-avatar-img" />
                    ) : (
                      <div className="user-avatar-circle">{initials}</div>
                    )}
                    <div>
                      <strong>{c.name}</strong>
                      <br />
                      <small className="muted">{c.email}</small>
                    </div>
                  </div>
                )
              },
            },
            {
              header: 'Employee ID',
              render: (c) => (
                <span className="badge badge-gray" style={{ fontFamily: 'monospace' }}>
                  <Hash size={12} style={{ marginRight: 2, verticalAlign: -1 }} />
                  {c.employeeId || '—'}
                </span>
              ),
            },
            {
              header: 'Mobile',
              render: (c) => c.phone || <span className="muted" style={{ fontStyle: 'italic' }}>Not provided</span>,
            },
            {
              header: 'Status',
              render: (c) => (
                <span className={`badge ${c.enabled ? 'badge-green' : 'badge-gray'}`}>
                  {c.enabled ? 'Active' : 'Deactivated'}
                </span>
              ),
            },
            {
              header: 'Actions',
              className: 'right',
              render: (c) => (
                <div style={{ display: 'inline-flex', gap: '0.4rem', justifyContent: 'flex-end' }}>
                  <button
                    className="btn btn-ghost"
                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.82rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                    onClick={() => handleViewSales(c)}
                    title={`View sales created by ${c.name}`}
                  >
                    <History size={14} /> Sales
                  </button>
                  <button
                    className="btn btn-ghost"
                    style={{ padding: '0.35rem 0.65rem', fontSize: '0.82rem', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                    onClick={() => handleOpenEdit(c)}
                    title={`Edit ${c.name}`}
                  >
                    <Pencil size={14} /> Edit
                  </button>
                  <button
                    className="btn btn-ghost"
                    style={{
                      padding: '0.35rem 0.65rem',
                      fontSize: '0.82rem',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                      color: c.enabled ? 'var(--red)' : 'var(--green)',
                    }}
                    onClick={() => setDeactivatingCashier(c)}
                    title={c.enabled ? `Deactivate ${c.name}` : `Activate ${c.name}`}
                  >
                    {c.enabled ? <ShieldOff size={14} /> : <ShieldCheck size={14} />}
                    {c.enabled ? 'Deactivate' : 'Activate'}
                  </button>
                </div>
              ),
            },
          ]}
        />
      </div>

      {/* Edit Cashier Modal */}
      {editingCashier && (
        <Modal title={`Edit Cashier: ${editingCashier.name}`} onClose={() => setEditingCashier(null)}>
          {editErrors.api && <div className="alert-error" style={{ marginBottom: '1rem' }}>{editErrors.api}</div>}
          <form onSubmit={handleSaveEdit} noValidate>
            <Field label="Full Name" error={editErrors.name} required>
              <input
                className="input"
                type="text"
                value={editForm.name}
                onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                autoFocus
              />
            </Field>

            <Field label="Mobile Number" error={editErrors.phone}>
              <input
                className="input"
                type="tel"
                value={editForm.phone}
                onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                placeholder="+91 98765 43210"
              />
            </Field>

            <Field label="Employee ID (Fixed)">
              <div className="readonly-field-box">
                <Hash size={16} className="muted" />
                <span>{editingCashier.employeeId || '—'}</span>
                <span className="readonly-pill"><Lock size={12} /> Fixed</span>
              </div>
            </Field>

            <Field label="Email Address (Fixed)">
              <div className="readonly-field-box">
                <Mail size={16} className="muted" />
                <span>{editingCashier.email}</span>
                <span className="readonly-pill"><Lock size={12} /> Fixed</span>
              </div>
            </Field>

            <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
              <Button type="button" variant="ghost" onClick={() => setEditingCashier(null)} disabled={editSaving}>
                Cancel
              </Button>
              <Button type="submit" loading={editSaving}>
                <Save size={16} /> Save Changes
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Cashier Sales History Modal */}
      {salesCashier && (
        <Modal
          title={`Sales History: ${salesCashier.name} (${salesCashier.employeeId || 'STAFF'})`}
          onClose={() => setSalesCashier(null)}
        >
          <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="muted" style={{ fontSize: '0.9rem' }}>
              Showing all recorded bills attributed to this cashier.
            </span>
            <Link
              to={`/sales?cashierId=${salesCashier.id}`}
              className="btn btn-ghost"
              style={{ fontSize: '0.82rem', padding: '0.3rem 0.6rem' }}
            >
              Open in full Sales History &rarr;
            </Link>
          </div>

          {salesLoading ? (
            <Spinner />
          ) : salesError ? (
            <div className="alert-error">{salesError}</div>
          ) : (
            <DataTable
              rows={cashierSales}
              empty={
                <EmptyState
                  title="No sales found"
                  text={`${salesCashier.name} has not created any bills yet.`}
                />
              }
              columns={[
                {
                  header: 'Invoice',
                  render: (s) => (
                    <Link to={`/sales/${s.id}`} style={{ fontWeight: 600 }}>
                      {s.invoiceNumber}
                    </Link>
                  ),
                },
                {
                  header: 'Date & Time',
                  render: (s) => formatDate(s.createdAt),
                },
                {
                  header: 'Payment',
                  render: (s) => (
                    <span className="badge badge-gray">
                      {s.paymentMethod || 'CASH'}
                    </span>
                  ),
                },
                {
                  header: 'Total',
                  className: 'right',
                  render: (s) => <strong>{formatMoney(s.totalAmount)}</strong>,
                },
                {
                  header: 'Action',
                  className: 'right',
                  render: (s) => (
                    <Link to={`/sales/${s.id}`} className="icon-btn" title="View Invoice">
                      <Eye size={16} />
                    </Link>
                  ),
                },
              ]}
            />
          )}

          <div className="modal-actions" style={{ marginTop: '1.25rem' }}>
            <Button variant="ghost" onClick={() => setSalesCashier(null)}>
              Close
            </Button>
          </div>
        </Modal>
      )}

      {/* Deactivate/Delete Cashier Confirmation Modal */}
      {deactivatingCashier && (
        <Modal
          title={deactivatingCashier.enabled ? `Deactivate ${deactivatingCashier.name}?` : `Reactivate ${deactivatingCashier.name}?`}
          onClose={() => setDeactivatingCashier(null)}
        >
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start', margin: '0.5rem 0 1.25rem' }}>
            <div style={{ width: 44, height: 44, borderRadius: '50%', background: '#fee2e2', color: 'var(--red)', display: 'grid', placeItems: 'center', flexShrink: 0 }}>
              <AlertTriangle size={24} />
            </div>
            <div>
              <p style={{ margin: '0 0 0.5rem 0', fontWeight: 600, color: 'var(--navy)' }}>
                {deactivatingCashier.enabled
                  ? `Are you sure you want to deactivate ${deactivatingCashier.name} (${deactivatingCashier.employeeId || deactivatingCashier.email})?`
                  : `Are you sure you want to reactivate ${deactivatingCashier.name}?`}
              </p>
              <p className="muted" style={{ margin: 0, fontSize: '0.88rem' }}>
                {deactivatingCashier.enabled
                  ? 'Historical sales, invoices, and audit records will remain fully intact and accessible. Deactivation prevents this cashier from logging into OmniCart.'
                  : 'Reactivating will allow this cashier to log in again with their existing credentials.'}
              </p>
            </div>
          </div>

          <div className="modal-actions">
            <Button variant="ghost" onClick={() => setDeactivatingCashier(null)} disabled={deactivateBusy}>
              Cancel
            </Button>
            <Button
              variant={deactivatingCashier.enabled ? 'danger' : 'primary'}
              loading={deactivateBusy}
              onClick={handleConfirmDeactivate}
            >
              {deactivatingCashier.enabled ? 'Confirm Deactivation' : 'Confirm Activation'}
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}
