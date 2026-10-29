import { Link } from 'react-router-dom'
import { Boxes, Package, Receipt, History, AlertTriangle, BarChart3, Store } from 'lucide-react'

const features = [
  { icon: Package, title: 'Product Management', text: 'Add, edit and organise products by category with SKU, price and stock details.' },
  { icon: Boxes, title: 'Inventory Tracking', text: 'See available stock for every product and who can change it.' },
  { icon: Receipt, title: 'Fast Billing', text: 'Search, add to cart and print an invoice in seconds. Prices are checked on the server.' },
  { icon: History, title: 'Sales History', text: 'Every bill is saved with its items, so any invoice can be opened again.' },
  { icon: AlertTriangle, title: 'Low-Stock Alerts', text: 'Products at or below their threshold are highlighted on the dashboard and inventory.' },
  { icon: BarChart3, title: 'Sales Dashboard', text: 'Revenue, total sales and recent bills for the owner at a glance.' },
]
const steps = [
  ['Add Products', 'The owner creates categories and products with prices and starting stock.'],
  ['Manage Stock', 'Track quantities and set a low-stock threshold for each product.'],
  ['Generate Bills', 'Cashiers add items to a cart; stock drops automatically when the bill is saved.'],
  ['Monitor Sales', 'Review invoices and the dashboard to see how the shop is doing.'],
]

import { useAuth } from '../context/AuthContext'
import { homePath } from '../utils/format'
import { UserCircle, ArrowRight } from 'lucide-react'

export default function LandingPage() {
  const { user } = useAuth()

  return (
    <div>
      <header className="l-nav">
        <div className="l-nav-inner">
          <a href="#home" className="logo"><Store size={22} color="#4f46e5" /> OmniCart</a>
          <nav className="l-links">
            <a href="#home">Home</a><a href="#features">Features</a><a href="#how-it-works">How It Works</a><a href="#about">About</a>
          </nav>
          <div className="l-cta">
            {user ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                <Link to="/profile" className="btn btn-ghost" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: '0.45rem 0.75rem' }}>
                  <UserCircle size={18} /> <span>{user.name}</span> <span className="badge badge-gray" style={{ fontSize: '0.72rem', padding: '1px 6px' }}>{user.displayRole || (user.role === 'ADMIN' ? 'Owner' : 'Cashier')}</span>
                </Link>
                <Link to={homePath(user.role)} className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                  Go to Dashboard <ArrowRight size={16} />
                </Link>
              </div>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost">Login</Link>
                <Link to="/register" className="btn btn-primary">Get Started</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section id="home" className="l-section l-hero">
        <div>
          <h1>Manage Your Store. Simplify Every Sale.</h1>
          <p className="lead">OmniCart helps a small retail shop keep track of products and stock, create customer bills in seconds, and review sales history, all in one place.</p>
          <div className="btns">
            {user ? (
              <Link to={homePath(user.role)} className="btn btn-primary btn-lg" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                Go to Dashboard <ArrowRight size={18} />
              </Link>
            ) : (
              <Link to="/register" className="btn btn-primary btn-lg">Get Started</Link>
            )}
            <a href="#features" className="btn btn-ghost btn-lg">Explore Features</a>
          </div>
        </div>
        <div className="mock" aria-hidden="true">
          <div className="mock-bar"><i /><i /><i /></div>
          <div className="mock-body">
            <div className="mock-col">
              <strong style={{ fontSize: '.85rem' }}>Products</strong>
              {[['Basmati Rice 5kg', '₹650', '40 in stock'], ['Glucose Biscuits 800g', '₹80', '50 in stock'], ['Toor Dal 1kg', '₹165', 'Low stock'], ['Notebook A4', '₹60', '80 in stock']].map(([n, p, s]) => (
                <div className="mock-row" key={n}><span>{n}<br /><small className="muted">{s}</small></span><b>{p}</b></div>
              ))}
            </div>
            <div className="mock-col">
              <strong style={{ fontSize: '.85rem' }}>Current bill</strong>
              <div className="mock-row"><span>Basmati Rice 5kg × 2</span><b>₹1,300</b></div>
              <div className="mock-row"><span>Notebook A4 × 5</span><b>₹300</b></div>
              <div className="mock-total"><span>Total</span><span>₹1,600</span></div>
              <div className="mock-pay">Generate bill</div>
            </div>
          </div>
        </div>
      </section>

      <div className="l-band">
        <section id="features" className="l-section">
          <div className="l-title"><h2>Everything a small shop needs, nothing it doesn't</h2><p className="muted">Six focused features that cover the daily work of running a counter.</p></div>
          <div className="feature-grid">
            {features.map(({ icon: Icon, title, text }) => (
              <div className="card feature" key={title}>
                <div className="stat-icon"><Icon size={20} /></div>
                <h3>{title}</h3><p className="muted" style={{ marginBottom: 0 }}>{text}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section id="how-it-works" className="l-section">
        <div className="l-title"><h2>How it works</h2></div>
        <div className="steps">
          {steps.map(([t, d]) => <div className="step" key={t}><h3>{t}</h3><p className="muted">{d}</p></div>)}
        </div>
      </section>

      <div className="l-band">
        <section id="about" className="l-section">
          <div className="l-title" style={{ marginBottom: 0 }}>
            <h2>About OmniCart</h2>
            <p className="muted">OmniCart is a college Industry Practice project. The interface is built with React, the server with Spring Boot (REST APIs, Spring Security with JWT, JPA/Hibernate) and the data is stored in MySQL.</p>
            <Link to="/login" className="btn btn-primary">Try the demo</Link>
          </div>
        </section>
      </div>

      <footer className="l-footer">
        <div className="l-footer-inner">
          <div style={{ maxWidth: 340 }}><span className="logo"><Store size={20} /> OmniCart</span><p style={{ marginTop: '.6rem' }}>Inventory and billing management for small retail shops. A college Industry Practice project.</p></div>
          <div className="l-footer-links">
            <a href="#home">Home</a><a href="#features">Features</a><a href="#how-it-works">How It Works</a><a href="#about">About</a><Link to="/login">Login</Link><Link to="/register">Register</Link>
          </div>
        </div>
      </footer>
    </div>
  )
}
