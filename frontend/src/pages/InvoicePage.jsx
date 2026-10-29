import { Link, useLocation, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, Printer, AlertTriangle } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import { Button, ErrorState, Spinner } from '../components/ui'
import { formatDate, formatMoney } from '../utils/format'

export default function InvoicePage() {
  const { id } = useParams()
  const { state } = useLocation()
  const { data: sale, loading, error, reload } = useFetch(async () => (await api.get(`/api/sales/${id}`)).data, [id])

  if (loading) return <Spinner />
  if (error) return <ErrorState message={error} onRetry={reload} />

  return (
    <>
      <div className="no-print page-head">
        <Link to="/sales" className="btn btn-ghost"><ArrowLeft size={16} /> Sales history</Link>
        <Button onClick={() => window.print()}><Printer size={16} /> Print Invoice</Button>
      </div>
      {state?.justCreated && <div className="alert-success no-print"><CheckCircle2 size={18} /> Bill generated and stock updated.</div>}
      {sale.voided && (
        <div className="alert-error no-print" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertTriangle size={18} />
          <span><strong>VOIDED INVOICE:</strong> This sale has been deleted/voided by the Shop Owner and is excluded from active sales.</span>
        </div>
      )}
      <div className="invoice" style={sale.voided ? { opacity: 0.85, position: 'relative' } : {}}>
        {sale.voided && (
          <div style={{ textAlign: 'center', margin: '0 0 1rem 0', padding: '0.45rem', background: '#fee2e2', border: '1px dashed var(--red)', color: 'var(--red)', fontWeight: 700, borderRadius: '6px', fontSize: '0.85rem' }}>
            *** VOIDED INVOICE — CANCELLED BY SHOP OWNER ***
          </div>
        )}
        <div className="invoice-head">
          <div><h1 style={{ marginBottom: 0 }}>OmniCart</h1><span className="muted">Invoice</span></div>
          <div className="right"><strong>{sale.invoiceNumber}</strong><br /><span className="muted">{formatDate(sale.createdAt)}</span><br /><span className="muted">Cashier: {sale.cashierName}</span></div>
        </div>
        <table className="table">
          <thead><tr><th>Product</th><th className="right">Qty</th><th className="right">Unit price</th><th className="right">Line total</th></tr></thead>
          <tbody>
            {sale.items.map((i) => (
              <tr key={i.productId}><td>{i.productName}</td><td className="right">{i.quantity}</td><td className="right">{formatMoney(i.unitPrice)}</td><td className="right">{formatMoney(i.lineTotal)}</td></tr>
            ))}
          </tbody>
        </table>
        <div className="totals"><span>Total</span><span>{formatMoney(sale.totalAmount)}</span></div>

        <div className="invoice-payment-details">
          <div className="invoice-pay-row">
            <span>Payment Mode:</span>
            <strong>
              {sale.paymentMethod === 'UPI'
                ? 'UPI / QR (Demo)'
                : sale.paymentMethod === 'CARD'
                ? 'Card Payment (Demo)'
                : 'Cash'}
            </strong>
          </div>
          <div className="invoice-pay-row">
            <span>Payment Status:</span>
            <span className="badge badge-green">{sale.paymentStatus || 'PAID'}</span>
          </div>

          {sale.paymentMethod === 'CASH' && sale.amountPaid != null && (
            <>
              <div className="invoice-pay-row">
                <span>Cash Received:</span>
                <span>{formatMoney(sale.amountPaid)}</span>
              </div>
              <div className="invoice-pay-row">
                <span>Change Returned:</span>
                <span>{formatMoney(sale.changeDue || 0)}</span>
              </div>
            </>
          )}

          {(sale.paymentMethod === 'UPI' || sale.paymentMethod === 'CARD') && (
            <div className="invoice-demo-tag">
              Simulated Demo Transaction — Approved &amp; Settled
            </div>
          )}
        </div>

        <p className="muted" style={{ marginBottom: 0, marginTop: '1.2rem', textAlign: 'center' }}>Thank you for shopping with us!</p>
      </div>
    </>
  )
}
