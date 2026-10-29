import { AlertTriangle, Inbox, Loader2, X, ChevronLeft, ChevronRight } from 'lucide-react'

export function Spinner({ full }) {
  return (
    <div className={full ? 'center-screen' : 'center-block'}>
      <Loader2 className="spin" size={28} />
    </div>
  )
}

export function Button({ variant = 'primary', loading, children, ...props }) {
  return (
    <button className={`btn btn-${variant}`} disabled={loading || props.disabled} {...props}>
      {loading && <Loader2 className="spin" size={16} />}
      {children}
    </button>
  )
}

export function Field({ label, error, children }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}

export function EmptyState({ title, text, action }) {
  return (
    <div className="empty">
      <Inbox size={34} />
      <h3>{title}</h3>
      {text && <p>{text}</p>}
      {action}
    </div>
  )
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="empty error-box">
      <AlertTriangle size={30} />
      <h3>Could not load data</h3>
      <p>{message}</p>
      {onRetry && <Button variant="ghost" onClick={onRetry}>Try again</Button>}
    </div>
  )
}

export function Modal({ title, onClose, children }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-modal="true" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Close"><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function ConfirmDialog({ title, message, confirmLabel = 'Delete', loading, onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="muted">{message}</p>
      <div className="modal-actions">
        <Button variant="ghost" onClick={onCancel}>Cancel</Button>
        <Button variant="danger" loading={loading} onClick={onConfirm}>{confirmLabel}</Button>
      </div>
    </Modal>
  )
}

export function StatusBadge({ status }) {
  const map = { IN_STOCK: ['In Stock', 'green'], LOW_STOCK: ['Low Stock', 'amber'], OUT_OF_STOCK: ['Out of Stock', 'red'] }
  const [label, color] = map[status] || [status, 'gray']
  return <span className={`badge badge-${color}`}>{label}</span>
}

/** columns: [{ header, render: (row) => node, className? }] */
export function DataTable({ columns, rows, empty }) {
  if (!rows.length) return empty
  return (
    <div className="table-wrap">
      <table className="table">
        <thead><tr>{columns.map((c) => <th key={c.header} className={c.className}>{c.header}</th>)}</tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>{columns.map((c) => <td key={c.header} className={c.className}>{c.render(r)}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="pagination">
      <button className="icon-btn" disabled={page === 0} onClick={() => onChange(page - 1)} aria-label="Previous page"><ChevronLeft size={18} /></button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="icon-btn" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)} aria-label="Next page"><ChevronRight size={18} /></button>
    </div>
  )
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-head">
      <div><h1>{title}</h1>{subtitle && <p className="muted">{subtitle}</p>}</div>
      <div className="page-actions">{children}</div>
    </div>
  )
}

export { default as MultiSelectCategoryFilter } from './MultiSelectCategoryFilter'
export { default as PaymentModal } from './PaymentModal'
