import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Minus, Plus, Trash2, ShoppingCart, CreditCard } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import useDebounce from '../hooks/useDebounce'
import { useToast } from '../context/ToastContext'
import { Button, EmptyState, ErrorState, PageHeader, PaymentModal, Spinner } from '../components/ui'
import { errorMessage, formatMoney } from '../utils/format'

export default function BillingPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const search = useDebounce(query)
  const [cart, setCart] = useState([])   // [{ product, quantity }]
  const [busy, setBusy] = useState(false)
  const [showPayment, setShowPayment] = useState(false)

  const { data, loading, error, reload } = useFetch(
    async () => (await api.get('/api/products', { params: { search, size: 100 } })).data.content, [search])

  const qtyInCart = (id) => cart.find((l) => l.product.id === id)?.quantity ?? 0

  const add = (product) => {
    const current = qtyInCart(product.id)
    if (current + 1 > product.stockQuantity) { toast.error(`Only ${product.stockQuantity} of ${product.name} in stock`); return }
    setCart(current ? cart.map((l) => (l.product.id === product.id ? { ...l, quantity: current + 1 } : l)) : [...cart, { product, quantity: 1 }])
  }
  const change = (id, delta) => {
    const line = cart.find((l) => l.product.id === id)
    const next = line.quantity + delta
    if (next > line.product.stockQuantity) { toast.error(`Only ${line.product.stockQuantity} in stock`); return }
    setCart(next <= 0 ? cart.filter((l) => l.product.id !== id) : cart.map((l) => (l.product.id === id ? { ...l, quantity: next } : l)))
  }
  const remove = (id) => setCart(cart.filter((l) => l.product.id !== id))

  // Display-only estimate. The backend recalculates everything from database prices.
  const subtotal = cart.reduce((sum, l) => sum + l.product.sellingPrice * l.quantity, 0)
  const totalUnits = cart.reduce((sum, l) => sum + l.quantity, 0)

  // Only called AFTER the cashier confirms payment in the PaymentModal
  const handleConfirmCheckout = async ({ paymentMethod, amountPaid }) => {
    setBusy(true)
    try {
      // Product IDs, quantities, payment method and amount are sent
      const payload = {
        items: cart.map((l) => ({ productId: l.product.id, quantity: l.quantity })),
        paymentMethod,
        amountPaid: amountPaid != null ? Number(amountPaid) : undefined
      }
      const res = await api.post('/api/sales', payload)
      toast.success(`Bill ${res.data.invoiceNumber} generated`)
      setCart([])
      setShowPayment(false)
      navigate(`/sales/${res.data.id}`, { state: { justCreated: true } })
    } catch (err) {
      toast.error(errorMessage(err))
      reload()   // refresh stock numbers after a failure
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title="Billing" subtitle="Search products, build the cart, choose payment and generate a bill" />
      <div className="pos">
        <div className="card">
          <input className="input" style={{ marginBottom: '.8rem' }} placeholder="Search by product name or SKU" value={query} onChange={(e) => setQuery(e.target.value)} autoFocus />
          {loading ? <Spinner /> : error ? <ErrorState message={error} onRetry={reload} /> : !data.length ? <EmptyState title="No matching products" /> : (
            <div className="product-list">
              {data.map((p) => (
                <div className="product-row" key={p.id}>
                  <div><strong>{p.name}</strong><small>{p.sku} · {formatMoney(p.sellingPrice)} · {p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : 'Out of stock'}</small></div>
                  <Button variant="ghost" disabled={p.stockQuantity <= qtyInCart(p.id)} onClick={() => add(p)}><Plus size={15} /> Add</Button>
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="card">
          <h2><ShoppingCart size={18} style={{ verticalAlign: '-3px' }} /> Current bill</h2>
          {!cart.length ? <EmptyState title="Cart is empty" text="Add products from the list." /> : (
            <>
              {cart.map(({ product, quantity }) => (
                <div className="cart-line" key={product.id}>
                  <div><strong>{product.name}</strong><br /><small className="muted">{formatMoney(product.sellingPrice)} × {quantity} · {product.stockQuantity} available</small></div>
                  <div className="qty">
                    <button className="icon-btn" onClick={() => change(product.id, -1)} aria-label="Decrease quantity"><Minus size={15} /></button>
                    <span>{quantity}</span>
                    <button className="icon-btn" onClick={() => change(product.id, 1)} aria-label="Increase quantity"><Plus size={15} /></button>
                  </div>
                  <div><strong>{formatMoney(product.sellingPrice * quantity)}</strong> <button className="icon-btn" onClick={() => remove(product.id)} aria-label={`Remove ${product.name}`}><Trash2 size={15} /></button></div>
                </div>
              ))}
              <div className="totals">
                <span>Total payable ({totalUnits} item{totalUnits !== 1 ? 's' : ''})</span>
                <span>{formatMoney(subtotal)}</span>
              </div>
              <Button
                onClick={() => setShowPayment(true)}
                disabled={!cart.length || busy}
                style={{ width: '100%' }}
              >
                <CreditCard size={16} /> Proceed to Payment
              </Button>
            </>
          )}
        </div>
      </div>

      {showPayment && (
        <PaymentModal
          total={subtotal}
          itemCount={totalUnits}
          busy={busy}
          onClose={() => setShowPayment(false)}
          onConfirmPayment={handleConfirmCheckout}
        />
      )}
    </>
  )
}

