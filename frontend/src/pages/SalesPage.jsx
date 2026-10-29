import { useState, useEffect } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Eye, Trash2, AlertTriangle, CheckCircle2 } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import useDebounce from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { Button, DataTable, EmptyState, ErrorState, Modal, PageHeader, Pagination, Spinner } from '../components/ui'
import { formatDate, formatMoney, errorMessage } from '../utils/format'

export default function SalesPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialCashierId = searchParams.get('cashierId') || ''

  const [invoice, setInvoice] = useState('')
  const q = useDebounce(invoice)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [selectedCashier, setSelectedCashier] = useState(initialCashierId)
  const [page, setPage] = useState(0)

  // Cashiers list for Owner filter dropdown
  const [cashierList, setCashierList] = useState([])

  // Deletion modal state (Owner only)
  const [deletingSale, setDeletingSale] = useState(null)
  const [deleteBusy, setDeleteBusy] = useState(false)
  const [feedback, setFeedback] = useState({ text: '', type: '' })

  const isOwner = user?.role === 'ADMIN'

  // Load cashiers for Owner filter
  useEffect(() => {
    if (isOwner) {
      api.get('/api/cashiers')
        .then((res) => setCashierList(res.data))
        .catch(() => {})
    }
  }, [isOwner])

  // Sync state if query param changes
  useEffect(() => {
    const cid = searchParams.get('cashierId') || ''
    setSelectedCashier(cid)
  }, [searchParams])

  const { data, loading, error, reload } = useFetch(
    async () =>
      (
        await api.get('/api/sales', {
          params: {
            invoice: q,
            from: from || undefined,
            to: to || undefined,
            cashierId: selectedCashier || undefined,
            page,
            size: 10,
          },
        })
      ).data,
    [q, from, to, selectedCashier, page]
  )

  const showFeedback = (text, type = 'success') => {
    setFeedback({ text, type })
    setTimeout(() => setFeedback({ text: '', type: '' }), 5000)
  }

  const handleCashierFilterChange = (e) => {
    const val = e.target.value
    setSelectedCashier(val)
    setPage(0)
    if (val) {
      setSearchParams({ cashierId: val })
    } else {
      setSearchParams({})
    }
  }

  const handleConfirmDelete = async () => {
    if (!deletingSale) return
    setDeleteBusy(true)
    try {
      await api.delete(`/api/sales/${deletingSale.id}`)
      showFeedback(`Sale ${deletingSale.invoiceNumber} was successfully deleted.`)
      setDeletingSale(null)
      reload()
    } catch (err) {
      showFeedback(errorMessage(err), 'error')
    } finally {
      setDeleteBusy(false)
    }
  }

  return (
    <>
      <PageHeader
        title="Sales History"
        subtitle={isOwner ? 'All bills generated in the shop (Active sales)' : 'Bills you have generated'}
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

      <div className="toolbar" style={{ flexWrap: 'wrap' }}>
        <input
          className="input"
          placeholder="Search invoice number"
          value={invoice}
          onChange={(e) => {
            setInvoice(e.target.value)
            setPage(0)
          }}
          style={{ minWidth: 200 }}
        />

        {isOwner && (
          <select
            className="input"
            value={selectedCashier}
            onChange={handleCashierFilterChange}
            aria-label="Filter by cashier"
            style={{ minWidth: 160 }}
          >
            <option value="">All Cashiers</option>
            {cashierList.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} {c.employeeId ? `(${c.employeeId})` : ''}
              </option>
            ))}
          </select>
        )}

        <input
          className="input"
          type="date"
          aria-label="From date"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value)
            setPage(0)
          }}
        />
        <input
          className="input"
          type="date"
          aria-label="To date"
          value={to}
          onChange={(e) => {
            setTo(e.target.value)
            setPage(0)
          }}
        />
      </div>

      <div className="card">
        {loading ? (
          <Spinner />
        ) : error ? (
          <ErrorState message={error} onRetry={reload} />
        ) : (
          <>
            <DataTable
              rows={data.content}
              empty={<EmptyState title="No sales found" text="Generate a bill or change the filters." />}
              columns={[
                {
                  header: 'Invoice',
                  render: (s) => <strong>{s.invoiceNumber}</strong>,
                },
                {
                  header: 'Date',
                  render: (s) => formatDate(s.createdAt),
                },
                {
                  header: 'Cashier',
                  render: (s) => s.cashierName,
                },
                {
                  header: 'Items',
                  render: (s) => s.itemCount,
                },
                {
                  header: 'Payment',
                  render: (s) => (
                    <span className="badge badge-gray">{s.paymentMethod || 'CASH'}</span>
                  ),
                },
                {
                  header: 'Total',
                  className: 'right',
                  render: (s) => formatMoney(s.totalAmount),
                },
                {
                  header: 'Action',
                  className: 'right',
                  render: (s) => (
                    <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                      <Link to={`/sales/${s.id}`} className="icon-btn" aria-label={`View ${s.invoiceNumber}`} title="View Invoice">
                        <Eye size={17} />
                      </Link>
                      {isOwner && (
                        <button
                          type="button"
                          className="icon-btn"
                          style={{ color: 'var(--red)' }}
                          aria-label={`Delete ${s.invoiceNumber}`}
                          title="Delete Sale Record"
                          onClick={() => setDeletingSale(s)}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  ),
                },
              ]}
            />
            <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}
      </div>

      {/* Delete Sale Confirmation Modal (Owner Only) */}
      {deletingSale && (
        <Modal title="Delete Sale Record" onClose={() => setDeletingSale(null)}>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-start', margin: '0.5rem 0 1.25rem' }}>
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: '50%',
                background: '#fee2e2',
                color: 'var(--red)',
                display: 'grid',
                placeItems: 'center',
                flexShrink: 0,
              }}
            >
              <AlertTriangle size={24} />
            </div>
            <div>
              <p style={{ margin: '0 0 0.5rem 0', fontWeight: 600, color: 'var(--navy)' }}>
                Are you sure you want to delete invoice {deletingSale.invoiceNumber}?
              </p>
              <div
                style={{
                  background: '#f8fafc',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  fontSize: '0.88rem',
                  margin: '0.5rem 0',
                }}
              >
                <div><strong>Total Amount:</strong> {formatMoney(deletingSale.totalAmount)}</div>
                <div><strong>Cashier:</strong> {deletingSale.cashierName}</div>
                <div><strong>Date:</strong> {formatDate(deletingSale.createdAt)}</div>
              </div>
              <p className="muted" style={{ margin: 0, fontSize: '0.84rem' }}>
                This removes the sale from active sales history and updates dashboard totals. Historical inventory records and product details are preserved.
              </p>
            </div>
          </div>
          <div className="modal-actions">
            <Button variant="ghost" onClick={() => setDeletingSale(null)} disabled={deleteBusy}>
              Cancel
            </Button>
            <Button variant="danger" loading={deleteBusy} onClick={handleConfirmDelete}>
              Delete Sale
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}
