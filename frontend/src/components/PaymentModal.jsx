import { useState, useId } from 'react'
import {
  X,
  Banknote,
  QrCode,
  CreditCard,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ShieldCheck,
  RotateCcw
} from 'lucide-react'
import { Button } from './ui'
import { formatMoney } from '../utils/format'

/**
 * Professional POS Payment Selection & Confirmation Modal
 *
 * Supported Payment Modes:
 * 1. CASH: Cash received input, exact amount button, change due calculation
 * 2. UPI: Local simulated QR code with explicit simulation & confirmation steps
 * 3. CARD: Decorative card preview, masked inputs, demo simulation & confirmation
 *
 * Enforces: NO invoice or sale creation before explicit confirmation.
 */
export default function PaymentModal({
  total,
  itemCount,
  onClose,
  onConfirmPayment,
  busy = false
}) {
  const [method, setMethod] = useState('CASH') // 'CASH' | 'UPI' | 'CARD'

  // Cash state
  const [cashReceived, setCashReceived] = useState(total.toString())
  const numCashReceived = parseFloat(cashReceived) || 0
  const changeDue = Math.max(0, numCashReceived - total)
  const isCashSufficient = numCashReceived >= total

  // UPI state: 'PENDING' | 'SUCCESS'
  const [upiState, setUpiState] = useState('PENDING')
  const [upiRefId, setUpiRefId] = useState('')

  // Card state
  const [cardState, setCardState] = useState('PENDING') // 'PENDING' | 'SUCCESS'
  const [cardForm, setCardForm] = useState({
    name: 'Rahul Sharma',
    number: '4532 8921 7734 4242',
    expiry: '12/28',
    cvv: '888'
  })
  const [cardError, setCardError] = useState('')
  const [cardAuthCode, setCardAuthCode] = useState('')

  // Cash Shortcuts
  const handleExactCash = () => {
    setCashReceived(total.toFixed(2))
  }

  const handleAddCash = (amount) => {
    const current = parseFloat(cashReceived) || 0
    setCashReceived((current + amount).toFixed(2))
  }

  // Simulate UPI Success
  const handleSimulateUpiSuccess = () => {
    const demoRef = 'UPI-' + Math.floor(100000 + Math.random() * 900000)
    setUpiRefId(demoRef)
    setUpiState('SUCCESS')
  }

  // Card Formatting & Simulation
  const handleCardNumberChange = (e) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 16)
    const formatted = raw.match(/.{1,4}/g)?.join(' ') || raw
    setCardForm({ ...cardForm, number: formatted })
  }

  const handleExpiryChange = (e) => {
    let raw = e.target.value.replace(/\D/g, '').slice(0, 4)
    if (raw.length >= 2) raw = raw.slice(0, 2) + '/' + raw.slice(2)
    setCardForm({ ...cardForm, expiry: raw })
  }

  const handleFillDemoCard = () => {
    setCardForm({
      name: 'Priya Verma',
      number: '4111 2222 3333 9102',
      expiry: '09/29',
      cvv: '742'
    })
    setCardError('')
  }

  const handleSimulateCardPayment = () => {
    setCardError('')
    const cleanNumber = cardForm.number.replace(/\s/g, '')
    if (cleanNumber.length !== 16) {
      setCardError('Please enter a valid 16-digit card number')
      return
    }
    if (!cardForm.name.trim()) {
      setCardError('Cardholder name is required')
      return
    }
    if (!/^\d{2}\/\d{2}$/.test(cardForm.expiry)) {
      setCardError('Expiry date must be in MM/YY format')
      return
    }
    if (cardForm.cvv.length < 3) {
      setCardError('CVV must be 3 digits')
      return
    }

    const auth = 'AUTH-' + Math.floor(1000 + Math.random() * 9000)
    setCardAuthCode(auth)
    setCardState('SUCCESS')
  }

  // Final Confirmation Dispatcher
  const handleFinalConfirm = () => {
    if (method === 'CASH') {
      if (!isCashSufficient) return
      onConfirmPayment({
        paymentMethod: 'CASH',
        amountPaid: numCashReceived
      })
    } else if (method === 'UPI') {
      if (upiState !== 'SUCCESS') return
      onConfirmPayment({
        paymentMethod: 'UPI',
        amountPaid: total
      })
    } else if (method === 'CARD') {
      if (cardState !== 'SUCCESS') return
      onConfirmPayment({
        paymentMethod: 'CARD',
        amountPaid: total
      })
    }
  }

  const last4 = cardForm.number.replace(/\s/g, '').slice(-4) || '••••'

  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div
        className="modal payment-modal"
        role="dialog"
        aria-modal="true"
        onMouseDown={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="payment-modal-head">
          <div>
            <h2>Payment &amp; Checkout</h2>
            <p className="muted" style={{ margin: 0 }}>
              Select payment mode and confirm to generate the invoice ({itemCount} item{itemCount !== 1 ? 's' : ''})
            </p>
          </div>
          <button className="icon-btn" onClick={onClose} aria-label="Close payment dialog">
            <X size={20} />
          </button>
        </div>

        {/* Bill Summary Strip */}
        <div className="checkout-summary-strip">
          <span className="strip-label">Total Payable Amount</span>
          <span className="strip-value">{formatMoney(total)}</span>
        </div>

        {/* Payment Method Selector Cards */}
        <div className="payment-method-selector">
          <button
            type="button"
            className={`payment-method-card ${method === 'CASH' ? 'selected' : ''}`}
            onClick={() => setMethod('CASH')}
          >
            <div className="method-icon-wrap cash-icon">
              <Banknote size={22} />
            </div>
            <div className="method-details">
              <strong>Cash</strong>
              <small>Cash &amp; Change</small>
            </div>
          </button>

          <button
            type="button"
            className={`payment-method-card ${method === 'UPI' ? 'selected' : ''}`}
            onClick={() => setMethod('UPI')}
          >
            <div className="method-icon-wrap upi-icon">
              <QrCode size={22} />
            </div>
            <div className="method-details">
              <strong>UPI / QR</strong>
              <small>Simulated QR Pay</small>
            </div>
          </button>

          <button
            type="button"
            className={`payment-method-card ${method === 'CARD' ? 'selected' : ''}`}
            onClick={() => setMethod('CARD')}
          >
            <div className="method-icon-wrap card-icon">
              <CreditCard size={22} />
            </div>
            <div className="method-details">
              <strong>Card</strong>
              <small>Simulated POS</small>
            </div>
          </button>
        </div>

        {/* ===================== CASH PAYMENT PANEL ===================== */}
        {method === 'CASH' && (
          <div className="payment-tab-content">
            <div className="cash-calculator">
              <div className="calc-row">
                <label className="field-label" htmlFor="cash-received-input">
                  Cash Received from Customer (₹)
                </label>
                <div className="cash-input-wrap">
                  <span className="currency-prefix">₹</span>
                  <input
                    id="cash-received-input"
                    className="input cash-input"
                    type="number"
                    step="0.01"
                    min="0"
                    value={cashReceived}
                    onChange={(e) => setCashReceived(e.target.value)}
                    placeholder="Enter cash received"
                    autoFocus
                  />
                </div>
              </div>

              {/* Quick Cash Buttons */}
              <div className="cash-shortcuts">
                <button
                  type="button"
                  className="shortcut-btn"
                  onClick={handleExactCash}
                >
                  Exact Amount ({formatMoney(total)})
                </button>
                <button
                  type="button"
                  className="shortcut-btn"
                  onClick={() => handleAddCash(50)}
                >
                  +₹50
                </button>
                <button
                  type="button"
                  className="shortcut-btn"
                  onClick={() => handleAddCash(100)}
                >
                  +₹100
                </button>
                <button
                  type="button"
                  className="shortcut-btn"
                  onClick={() => handleAddCash(500)}
                >
                  +₹500
                </button>
              </div>

              {/* Change Due / Shortage Banner */}
              {numCashReceived >= total ? (
                <div className="change-result-box change-positive">
                  <div className="change-icon">
                    <CheckCircle2 size={24} />
                  </div>
                  <div>
                    <div className="change-title">Change Due to Customer</div>
                    <div className="change-amount">{formatMoney(changeDue)}</div>
                  </div>
                </div>
              ) : (
                <div className="change-result-box change-negative">
                  <div className="change-icon">
                    <AlertCircle size={24} />
                  </div>
                  <div>
                    <div className="change-title">Insufficient Cash Received</div>
                    <div className="change-amount">
                      Short by {formatMoney(total - numCashReceived)}
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
              <Button variant="ghost" type="button" onClick={onClose} disabled={busy}>
                Cancel
              </Button>
              <Button
                variant="primary"
                disabled={!isCashSufficient || busy}
                loading={busy}
                onClick={handleFinalConfirm}
              >
                Confirm Cash Payment &amp; Generate Invoice
              </Button>
            </div>
          </div>
        )}

        {/* ===================== UPI / QR PAYMENT PANEL ===================== */}
        {method === 'UPI' && (
          <div className="payment-tab-content">
            <div className="demo-notice-banner">
              <ShieldCheck size={16} />
              <span>
                <strong>Simulated UPI Demo:</strong> No real money will be transferred. Scan or click simulate below.
              </span>
            </div>

            {upiState === 'PENDING' ? (
              <div className="upi-qr-section">
                <div className="qr-card">
                  {/* Local Crisp Vector QR Graphic */}
                  <div className="qr-code-box">
                    <svg
                      viewBox="0 0 160 160"
                      width="150"
                      height="150"
                      className="qr-svg"
                      aria-label="Demo UPI QR Code"
                    >
                      <rect width="160" height="160" fill="#ffffff" />
                      {/* Top-Left Finder */}
                      <rect x="10" y="10" width="40" height="40" fill="#0f1b3d" rx="4" />
                      <rect x="18" y="18" width="24" height="24" fill="#ffffff" rx="2" />
                      <rect x="24" y="24" width="12" height="12" fill="#4f46e5" rx="1" />

                      {/* Top-Right Finder */}
                      <rect x="110" y="10" width="40" height="40" fill="#0f1b3d" rx="4" />
                      <rect x="118" y="18" width="24" height="24" fill="#ffffff" rx="2" />
                      <rect x="124" y="24" width="12" height="12" fill="#4f46e5" rx="1" />

                      {/* Bottom-Left Finder */}
                      <rect x="10" y="110" width="40" height="40" fill="#0f1b3d" rx="4" />
                      <rect x="18" y="118" width="24" height="24" fill="#ffffff" rx="2" />
                      <rect x="24" y="124" width="12" height="12" fill="#4f46e5" rx="1" />

                      {/* Simulated QR Data Pattern Pixels */}
                      <rect x="58" y="16" width="8" height="8" fill="#0f1b3d" />
                      <rect x="74" y="16" width="8" height="8" fill="#0f1b3d" />
                      <rect x="90" y="16" width="8" height="8" fill="#4f46e5" />
                      <rect x="58" y="32" width="16" height="8" fill="#4f46e5" />
                      <rect x="82" y="32" width="16" height="8" fill="#0f1b3d" />
                      <rect x="58" y="48" width="8" height="16" fill="#0f1b3d" />
                      <rect x="74" y="48" width="16" height="8" fill="#4f46e5" />

                      <rect x="16" y="58" width="8" height="16" fill="#4f46e5" />
                      <rect x="32" y="58" width="16" height="8" fill="#0f1b3d" />
                      <rect x="16" y="82" width="16" height="8" fill="#0f1b3d" />
                      <rect x="40" y="74" width="8" height="16" fill="#4f46e5" />

                      <rect x="118" y="58" width="16" height="8" fill="#0f1b3d" />
                      <rect x="118" y="74" width="8" height="16" fill="#4f46e5" />
                      <rect x="134" y="82" width="16" height="8" fill="#0f1b3d" />

                      {/* Center Badge */}
                      <rect x="62" y="62" width="36" height="36" fill="#4f46e5" rx="6" />
                      <text
                        x="80"
                        y="84"
                        fill="#ffffff"
                        fontSize="11"
                        fontWeight="bold"
                        textAnchor="middle"
                      >
                        UPI
                      </text>

                      {/* Bottom Pattern */}
                      <rect x="58" y="106" width="16" height="8" fill="#0f1b3d" />
                      <rect x="82" y="106" width="16" height="8" fill="#4f46e5" />
                      <rect x="58" y="122" width="8" height="16" fill="#4f46e5" />
                      <rect x="74" y="130" width="16" height="8" fill="#0f1b3d" />
                      <rect x="106" y="114" width="16" height="8" fill="#0f1b3d" />
                      <rect x="130" y="114" width="16" height="8" fill="#4f46e5" />
                      <rect x="114" y="130" width="16" height="16" fill="#0f1b3d" />
                    </svg>
                  </div>
                  <div className="qr-pay-text">
                    <strong>Scan with any UPI App</strong>
                    <span>Google Pay · PhonePe · Paytm · BHIM</span>
                  </div>
                  <div className="qr-amount-badge">
                    Amount: <strong>{formatMoney(total)}</strong>
                  </div>
                </div>

                <div className="upi-action-box">
                  <div className="upi-status-pill pending">
                    <span className="pulsing-dot" /> Waiting for payment scan...
                  </div>
                  <button
                    type="button"
                    className="btn btn-simulate"
                    onClick={handleSimulateUpiSuccess}
                  >
                    <Sparkles size={16} /> Simulate Payment Success
                  </button>
                  <small className="muted text-center">
                    Clicking above simulates customer scan approval.
                  </small>
                </div>
              </div>
            ) : (
              <div className="payment-success-card">
                <div className="success-icon-wrap">
                  <CheckCircle2 size={42} color="#15803d" />
                </div>
                <h3>UPI Payment Received (Simulated)</h3>
                <p className="muted">
                  Customer successfully transferred <strong>{formatMoney(total)}</strong>
                </p>
                <div className="ref-pill">Ref ID: {upiRefId}</div>
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => setUpiState('PENDING')}
                  style={{ marginTop: '.5rem', fontSize: '.85rem' }}
                >
                  <RotateCcw size={13} style={{ verticalAlign: '-1px' }} /> Reset / Scan Again
                </button>
              </div>
            )}

            <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
              <Button variant="ghost" type="button" onClick={onClose} disabled={busy}>
                Cancel
              </Button>
              <Button
                variant="primary"
                disabled={upiState !== 'SUCCESS' || busy}
                loading={busy}
                onClick={handleFinalConfirm}
              >
                Confirm Payment &amp; Generate Invoice
              </Button>
            </div>
          </div>
        )}

        {/* ===================== CARD PAYMENT PANEL ===================== */}
        {method === 'CARD' && (
          <div className="payment-tab-content">
            <div className="demo-notice-banner">
              <ShieldCheck size={16} />
              <span>
                <strong>Simulated Card POS:</strong> Dummy card details are never saved or transmitted.
              </span>
            </div>

            {/* Interactive Decorative Card Preview */}
            <div className="card-preview">
              <div className="card-preview-top">
                <span className="card-bank-name">OMNICART RETAIL CARD</span>
                <span className="card-brand-logo">VISA / RUPAY</span>
              </div>
              <div className="card-chip-symbol">
                <div className="chip" />
                <span className="contactless-icon">))))</span>
              </div>
              <div className="card-number-display">
                •••• •••• •••• {last4}
              </div>
              <div className="card-preview-bottom">
                <div>
                  <small>CARDHOLDER</small>
                  <strong>{cardForm.name.toUpperCase() || 'VALUED CUSTOMER'}</strong>
                </div>
                <div>
                  <small>EXPIRES</small>
                  <strong>{cardForm.expiry || 'MM/YY'}</strong>
                </div>
              </div>
              {cardState === 'SUCCESS' && (
                <div className="card-approved-stamp">
                  <CheckCircle2 size={16} /> APPROVED
                </div>
              )}
            </div>

            {cardState === 'PENDING' ? (
              <div className="card-form-grid">
                <div className="card-quick-fill">
                  <span>Need test data?</span>
                  <button
                    type="button"
                    className="btn-link"
                    onClick={handleFillDemoCard}
                  >
                    Fill Demo Card
                  </button>
                </div>

                {cardError && <div className="alert-error" role="alert">{cardError}</div>}

                <div className="field" style={{ marginBottom: '.6rem' }}>
                  <label className="field-label" htmlFor="cardholder-input">Cardholder Name</label>
                  <input
                    id="cardholder-input"
                    className="input"
                    type="text"
                    value={cardForm.name}
                    onChange={(e) => setCardForm({ ...cardForm, name: e.target.value })}
                    placeholder="Name on card"
                  />
                </div>

                <div className="field" style={{ marginBottom: '.6rem' }}>
                  <label className="field-label" htmlFor="cardnumber-input">Card Number (16 digits)</label>
                  <input
                    id="cardnumber-input"
                    className="input"
                    type="text"
                    value={cardForm.number}
                    onChange={handleCardNumberChange}
                    placeholder="4532 •••• •••• 4242"
                    maxLength={19}
                  />
                </div>

                <div className="card-row-split">
                  <div className="field">
                    <label className="field-label" htmlFor="expiry-input">Expiry Date</label>
                    <input
                      id="expiry-input"
                      className="input"
                      type="text"
                      value={cardForm.expiry}
                      onChange={handleExpiryChange}
                      placeholder="MM/YY"
                      maxLength={5}
                    />
                  </div>
                  <div className="field">
                    <label className="field-label" htmlFor="cvv-input">CVV</label>
                    <input
                      id="cvv-input"
                      className="input"
                      type="password"
                      value={cardForm.cvv}
                      onChange={(e) =>
                        setCardForm({
                          ...cardForm,
                          cvv: e.target.value.replace(/\D/g, '').slice(0, 3)
                        })
                      }
                      placeholder="•••"
                      maxLength={3}
                    />
                  </div>
                </div>

                <button
                  type="button"
                  className="btn btn-simulate"
                  style={{ width: '100%', marginTop: '.5rem' }}
                  onClick={handleSimulateCardPayment}
                >
                  <Sparkles size={16} /> Simulate Card Payment ({formatMoney(total)})
                </button>
              </div>
            ) : (
              <div className="payment-success-card" style={{ marginTop: '1rem' }}>
                <div className="success-icon-wrap">
                  <CheckCircle2 size={42} color="#15803d" />
                </div>
                <h3>Card Authorized &amp; Charged (Simulated)</h3>
                <p className="muted">
                  Transaction approved for <strong>{formatMoney(total)}</strong> on card ending in <strong>{last4}</strong>
                </p>
                <div className="ref-pill">Auth Code: {cardAuthCode}</div>
                <button
                  type="button"
                  className="btn-link"
                  onClick={() => setCardState('PENDING')}
                  style={{ marginTop: '.5rem', fontSize: '.85rem' }}
                >
                  <RotateCcw size={13} style={{ verticalAlign: '-1px' }} /> Use Different Card
                </button>
              </div>
            )}

            <div className="modal-actions" style={{ marginTop: '1.5rem' }}>
              <Button variant="ghost" type="button" onClick={onClose} disabled={busy}>
                Cancel
              </Button>
              <Button
                variant="primary"
                disabled={cardState !== 'SUCCESS' || busy}
                loading={busy}
                onClick={handleFinalConfirm}
              >
                Confirm Payment &amp; Generate Invoice
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
