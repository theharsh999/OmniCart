import { useState, useEffect, useCallback, useMemo } from 'react'
import { Link } from 'react-router-dom'
import {
  TrendingUp,
  TrendingDown,
  Minus,
  Receipt,
  ShoppingCart,
  DollarSign,
  PackageCheck,
  CreditCard,
  Layers,
  Award,
  Users,
  RefreshCw,
  Calendar,
  AlertCircle,
  Clock,
  ArrowUpRight
} from 'lucide-react'
import { fetchAnalyticsDashboard } from '../services/analyticsService'
import { formatMoney, formatDate } from '../utils/format'
import RevenueTrendChart from '../components/RevenueTrendChart'

const PRESET_FILTERS = [
  { key: 'today', label: 'Today' },
  { key: 'last7days', label: 'Last 7 Days' },
  { key: 'last30days', label: 'Last 30 Days' },
  { key: 'thisMonth', label: 'This Month' },
  { key: 'thisYear', label: 'This Year' },
  { key: 'custom', label: 'Custom Range' },
]

export default function SalesAnalyticsPage() {
  const [period, setPeriod] = useState('last7days')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [activeRangeLabel, setActiveRangeLabel] = useState('Last 7 Days')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [dashboardData, setDashboardData] = useState(null)

  const loadData = useCallback(async (selectedPeriod, fromDate, toDate) => {
    setLoading(true)
    setError(null)
    try {
      const params = {}
      if (selectedPeriod === 'custom' && fromDate && toDate) {
        params.from = fromDate
        params.to = toDate
        params.period = 'custom'
      } else {
        params.period = selectedPeriod || 'last7days'
      }

      const data = await fetchAnalyticsDashboard(params)
      setDashboardData(data)

      if (data?.summary?.from && data?.summary?.to) {
        setActiveRangeLabel(`${data.summary.from} to ${data.summary.to}`)
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to load sales analytics. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (period !== 'custom') {
      loadData(period, null, null)
    }
  }, [period, loadData])

  const handleApplyCustom = (e) => {
    e.preventDefault()
    if (!customFrom || !customTo) return
    loadData('custom', customFrom, customTo)
  }

  const summary = dashboardData?.summary || {}
  const paymentBreakdown = dashboardData?.paymentBreakdown || []
  const categoryBreakdown = dashboardData?.categoryBreakdown || []
  const topProductsByQuantity = dashboardData?.topProductsByQuantity || []
  const topProductsByRevenue = dashboardData?.topProductsByRevenue || []
  const cashierPerformance = dashboardData?.cashierPerformance || []
  const recentSales = dashboardData?.recentSales || []

  // Dynamic Insight: Top Payment Method
  const topPaymentInsight = useMemo(() => {
    const active = paymentBreakdown.filter((p) => p.totalRevenue > 0)
    if (active.length === 0) return null
    const top = active.reduce((m, p) => (p.totalRevenue > m.totalRevenue ? p : m), active[0])
    return {
      name: top.paymentMethod,
      pct: top.percentage,
      amount: top.totalRevenue,
      count: top.orderCount,
    }
  }, [paymentBreakdown])

  // Dynamic Insight: Top Category
  const topCategoryInsight = useMemo(() => {
    const active = categoryBreakdown.filter((c) => c.totalRevenue > 0)
    if (active.length === 0) return null
    const top = active.reduce((m, c) => (c.totalRevenue > m.totalRevenue ? c : m), active[0])
    return {
      name: top.categoryName,
      pct: top.percentage,
      amount: top.totalRevenue,
      units: top.itemsSold,
    }
  }, [categoryBreakdown])

  // Dynamic Insight: Top Performing Cashier
  const topCashierId = useMemo(() => {
    const active = cashierPerformance.filter((c) => c.totalRevenue > 0)
    if (active.length === 0) return null
    const top = active.reduce((m, c) => (c.totalRevenue > m.totalRevenue ? c : m), active[0])
    return top.cashierId
  }, [cashierPerformance])

  return (
    <div className="analytics-page">
      {/* Header and Controls */}
      <div className="page-head analytics-head">
        <div>
          <h1>Sales Analytics</h1>
          <p className="muted">
            Store performance and revenue trends • <span className="range-badge">{activeRangeLabel}</span>
          </p>
        </div>

        <div className="analytics-filters-bar">
          {/* Preset Period Filters */}
          <div className="preset-buttons-group">
            {PRESET_FILTERS.map((f) => (
              <button
                key={f.key}
                className={`filter-pill-btn ${period === f.key ? 'active' : ''}`}
                onClick={() => setPeriod(f.key)}
              >
                {f.label}
              </button>
            ))}
          </div>

          {/* Refresh Action */}
          <button
            className="btn btn-ghost icon-only-btn"
            onClick={() => loadData(period, customFrom, customTo)}
            disabled={loading}
            title="Refresh analytics data"
            aria-label="Refresh"
          >
            <RefreshCw size={16} className={loading ? 'spin' : ''} />
          </button>
        </div>
      </div>

      {/* Custom Date Range Picker form */}
      {period === 'custom' && (
        <form onSubmit={handleApplyCustom} className="card custom-date-card">
          <div className="custom-date-inputs">
            <div className="custom-date-field">
              <label>Start Date</label>
              <input
                type="date"
                className="input"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                required
              />
            </div>
            <div className="custom-date-field">
              <label>End Date</label>
              <input
                type="date"
                className="input"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                required
              />
            </div>
            <button
              type="submit"
              className="btn btn-primary apply-filter-btn"
              disabled={!customFrom || !customTo || loading}
            >
              <Calendar size={15} /> Apply Range
            </button>
          </div>
        </form>
      )}

      {/* Error Banner with Retry */}
      {error && (
        <div className="card error-box" style={{ marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} color="var(--red)" />
          <div style={{ flex: 1 }}>
            <strong>Server Error:</strong> {error}
          </div>
          <button className="btn btn-primary" style={{ padding: '0.35rem 0.85rem', fontSize: '0.82rem' }} onClick={() => loadData(period, customFrom, customTo)}>
            Retry
          </button>
        </div>
      )}

      {/* Loading State */}
      {loading && !dashboardData && (
        <div className="center-block" style={{ padding: '4rem 1rem' }}>
          <RefreshCw size={32} className="spin" color="var(--accent)" />
          <p style={{ marginTop: '0.75rem', fontWeight: 600 }}>Loading sales analytics...</p>
        </div>
      )}

      {dashboardData && (
        <>
          {/* Section A: KPI Cards (3 columns x 2 rows on desktop) */}
          <div className="analytics-kpi-grid">
            {/* 1. Total Revenue */}
            <div className="card kpi-card">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#eef2ff', color: '#4f46e5' }}>
                  <DollarSign size={20} />
                </div>
                <div className="kpi-value">{formatMoney(summary.totalRevenue)}</div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Total Revenue</div>
                <div className="kpi-desc">Revenue generated during the selected period</div>
              </div>
              <div className="kpi-footer">
                {summary.revenueGrowthPct !== null && summary.revenueGrowthPct !== undefined ? (
                  summary.revenueGrowthPct > 0 ? (
                    <span className="kpi-growth positive" title={`Previous period: ${formatMoney(summary.prevPeriodRevenue)}`}>
                      <TrendingUp size={11} /> +{summary.revenueGrowthPct}% vs prior period
                    </span>
                  ) : summary.revenueGrowthPct < 0 ? (
                    <span className="kpi-growth negative" title={`Previous period: ${formatMoney(summary.prevPeriodRevenue)}`}>
                      <TrendingDown size={11} /> {summary.revenueGrowthPct}% vs prior period
                    </span>
                  ) : (
                    <span className="kpi-growth neutral">
                      <Minus size={11} /> 0.0% vs prior period
                    </span>
                  )
                ) : (
                  <span className="kpi-growth-placeholder" />
                )}
              </div>
            </div>

            {/* 2. Total Orders */}
            <div className="card kpi-card">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#ecfdf5', color: '#059669' }}>
                  <Receipt size={20} />
                </div>
                <div className="kpi-value">{summary.totalOrders || 0}</div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Total Orders</div>
                <div className="kpi-desc">Completed bills during this period</div>
              </div>
              <div className="kpi-footer">
                {summary.ordersGrowthPct !== null && summary.ordersGrowthPct !== undefined ? (
                  summary.ordersGrowthPct > 0 ? (
                    <span className="kpi-growth positive" title={`Previous period: ${summary.prevPeriodOrders} bills`}>
                      <TrendingUp size={11} /> +{summary.ordersGrowthPct}% vs prior period
                    </span>
                  ) : summary.ordersGrowthPct < 0 ? (
                    <span className="kpi-growth negative" title={`Previous period: ${summary.prevPeriodOrders} bills`}>
                      <TrendingDown size={11} /> {summary.ordersGrowthPct}% vs prior period
                    </span>
                  ) : (
                    <span className="kpi-growth neutral">
                      <Minus size={11} /> 0.0% vs prior period
                    </span>
                  )
                ) : (
                  <span className="kpi-growth-placeholder" />
                )}
              </div>
            </div>

            {/* 3. Average Order Value */}
            <div className="card kpi-card">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#fef3c7', color: '#d97706' }}>
                  <TrendingUp size={20} />
                </div>
                <div className="kpi-value">{formatMoney(summary.averageOrderValue)}</div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Average Order Value</div>
                <div className="kpi-desc">Average revenue per bill</div>
              </div>
              <div className="kpi-footer">
                <span className="kpi-growth-placeholder" />
              </div>
            </div>

            {/* 4. Total Items Sold */}
            <div className="card kpi-card">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
                  <PackageCheck size={20} />
                </div>
                <div className="kpi-value">
                  {summary.totalItemsSold || 0} <span className="kpi-unit">units</span>
                </div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Total Items Sold</div>
                <div className="kpi-desc">Total units sold during this period</div>
              </div>
              <div className="kpi-footer">
                <span className="kpi-growth-placeholder" />
              </div>
            </div>

            {/* 5. Today's Revenue */}
            <div className="card kpi-card highlight-kpi">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#e0e7ff', color: '#3730a3' }}>
                  <Clock size={20} />
                </div>
                <div className="kpi-value">{formatMoney(summary.todayRevenue)}</div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Today's Revenue</div>
                <div className="kpi-desc">Revenue collected today</div>
              </div>
              <div className="kpi-footer">
                <span className="kpi-growth-placeholder" />
              </div>
            </div>

            {/* 6. Today's Orders */}
            <div className="card kpi-card highlight-kpi">
              <div className="kpi-card-top">
                <div className="kpi-icon-wrap" style={{ background: '#cffafe', color: '#0891b2' }}>
                  <ShoppingCart size={20} />
                </div>
                <div className="kpi-value">
                  {summary.todayOrders || 0} <span className="kpi-unit">bills</span>
                </div>
              </div>
              <div className="kpi-body">
                <div className="kpi-title">Today's Orders</div>
                <div className="kpi-desc">Bills generated today</div>
              </div>
              <div className="kpi-footer">
                <span className="kpi-growth-placeholder" />
              </div>
            </div>
          </div>

          {/* Section B: Interactive Revenue Trend Chart */}
          <div style={{ marginBottom: '1.5rem' }}>
            <RevenueTrendChart data={dashboardData.revenueTrend} />
          </div>

          {/* Section C: Sales Breakdowns (Payment Modes & Categories) */}
          <div className="grid-2" style={{ marginBottom: '1.5rem' }}>
            {/* Payment Method Breakdown */}
            <div className="card">
              <div className="card-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <CreditCard size={18} color="var(--accent)" />
                  <h3>Payment Mode Breakdown</h3>
                </div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>By payment method</span>
              </div>

              {topPaymentInsight && (
                <div className="analytics-insight-banner compact">
                  <CreditCard size={14} color="#4f46e5" style={{ flexShrink: 0 }} />
                  <div>
                    Most used payment method is <strong>{topPaymentInsight.name}</strong> ({topPaymentInsight.pct}% of revenue • {formatMoney(topPaymentInsight.amount)}).
                  </div>
                </div>
              )}

              {paymentBreakdown.length === 0 || paymentBreakdown.every((p) => p.totalRevenue === 0) ? (
                <div className="empty" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
                  <p className="muted">No payment records found for this period.</p>
                </div>
              ) : (
                <div className="breakdown-list">
                  {paymentBreakdown.map((item) => (
                    <div key={item.paymentMethod} className="breakdown-item">
                      <div className="breakdown-row">
                        <div className="breakdown-label">
                          <span className={`payment-pill ${item.paymentMethod.toLowerCase()}`}>
                            {item.paymentMethod}
                          </span>
                          <span className="muted" style={{ fontSize: '0.82rem' }}>
                            {item.orderCount} {item.orderCount === 1 ? 'bill' : 'bills'}
                          </span>
                        </div>
                        <div className="breakdown-values">
                          <strong>{formatMoney(item.totalRevenue)}</strong>
                          <span className="breakdown-pct">{item.percentage}%</span>
                        </div>
                      </div>
                      <div className="progress-bar-bg">
                        <div
                          className={`progress-bar-fill ${item.paymentMethod.toLowerCase()}`}
                          style={{ width: `${Math.min(Math.max(item.percentage, 0), 100)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Sales by Category */}
            <div className="card">
              <div className="card-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Layers size={18} color="var(--accent)" />
                  <h3>Sales by Product Category</h3>
                </div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>By category</span>
              </div>

              {topCategoryInsight && (
                <div className="analytics-insight-banner compact">
                  <Layers size={14} color="#4f46e5" style={{ flexShrink: 0 }} />
                  <div>
                    Top performing category is <strong>{topCategoryInsight.name}</strong>, generating <strong>{formatMoney(topCategoryInsight.amount)}</strong> ({topCategoryInsight.pct}% of total sales).
                  </div>
                </div>
              )}

              {categoryBreakdown.length === 0 ? (
                <div className="empty" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
                  <p className="muted">No category sales recorded for this period.</p>
                </div>
              ) : (
                <div className="breakdown-list">
                  {categoryBreakdown.map((cat, idx) => (
                    <div key={cat.categoryName} className="breakdown-item">
                      <div className="breakdown-row">
                        <div className="breakdown-label">
                          <strong>{cat.categoryName}</strong>
                          {idx === 0 && <span className="top-performer-badge">#1 Top Category</span>}
                          <span className="muted" style={{ fontSize: '0.82rem', marginLeft: '4px' }}>
                            ({cat.itemsSold} units sold)
                          </span>
                        </div>
                        <div className="breakdown-values">
                          <strong>{formatMoney(cat.totalRevenue)}</strong>
                          <span className="breakdown-pct">{cat.percentage}%</span>
                        </div>
                      </div>
                      <div className="progress-bar-bg">
                        <div
                          className="progress-bar-fill category-fill"
                          style={{ width: `${Math.min(Math.max(cat.percentage, 0), 100)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Section D: Top Products (by Quantity & by Revenue) */}
          <div className="grid-2" style={{ marginBottom: '1.5rem' }}>
            {/* Top 5 Products by Quantity Sold */}
            <div className="card">
              <div className="card-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Award size={18} color="#d97706" />
                  <h3>Top 5 Best-Selling Products</h3>
                </div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>Ranked by units sold</span>
              </div>

              {topProductsByQuantity.length === 0 ? (
                <div className="empty" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
                  <p className="muted">No products sold in this period.</p>
                </div>
              ) : (
                <div className="top-products-list">
                  {topProductsByQuantity.map((prod, idx) => (
                    <div key={prod.productId} className="top-product-row">
                      <div className={`rank-badge rank-${idx + 1}`}>#{idx + 1}</div>
                      <div className="top-product-info">
                        <span className="top-product-name">{prod.productName}</span>
                        <div className="top-product-meta">
                          <span className="product-sku-tag">{prod.sku}</span>
                          <span className="muted">• {prod.categoryName}</span>
                        </div>
                      </div>
                      <div className="top-product-stats">
                        <strong className="units-stat">{prod.quantitySold} units</strong>
                        <span className="muted" style={{ fontSize: '0.8rem' }}>{formatMoney(prod.totalRevenue)}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Top 5 Products by Revenue */}
            <div className="card">
              <div className="card-head">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <DollarSign size={18} color="#059669" />
                  <h3>Top 5 Products by Revenue</h3>
                </div>
                <span className="muted" style={{ fontSize: '0.8rem' }}>Ranked by total sales value</span>
              </div>

              {topProductsByRevenue.length === 0 ? (
                <div className="empty" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
                  <p className="muted">No products sold in this period.</p>
                </div>
              ) : (
                <div className="top-products-list">
                  {topProductsByRevenue.map((prod, idx) => (
                    <div key={prod.productId} className="top-product-row">
                      <div className={`rank-badge rank-${idx + 1}`}>#{idx + 1}</div>
                      <div className="top-product-info">
                        <span className="top-product-name">{prod.productName}</span>
                        <div className="top-product-meta">
                          <span className="product-sku-tag">{prod.sku}</span>
                          <span className="muted">• {prod.categoryName}</span>
                        </div>
                      </div>
                      <div className="top-product-stats">
                        <strong className="revenue-stat">{formatMoney(prod.totalRevenue)}</strong>
                        <span className="muted" style={{ fontSize: '0.8rem' }}>{prod.quantitySold} units sold</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Section E: Cashier Sales Performance Table */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-head">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Users size={18} color="var(--accent)" />
                <h3>Cashier Performance</h3>
              </div>
              <span className="muted" style={{ fontSize: '0.8rem' }}>Billing contributions by cashier</span>
            </div>

            {cashierPerformance.length === 0 ? (
              <div className="empty" style={{ padding: '2.5rem 1.5rem', textAlign: 'center' }}>
                <p className="muted">No cashier sales recorded in this date range.</p>
              </div>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Cashier Name</th>
                      <th>Employee ID</th>
                      <th className="right">Bills Generated</th>
                      <th className="right">Items Sold</th>
                      <th className="right">Total Revenue</th>
                      <th className="right">Average Bill Value</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cashierPerformance.map((c) => {
                      const isTop = c.cashierId === topCashierId
                      return (
                        <tr key={c.cashierId} style={isTop ? { background: '#fdfcfe' } : {}}>
                          <td>
                            <strong>{c.cashierName}</strong>
                            {isTop && <span className="top-performer-badge">🏆 Top Performer</span>}
                          </td>
                          <td>
                            <span className="empid-tag" style={{ background: '#f1f5f9', color: '#475569' }}>
                              {c.employeeId || 'STAFF'}
                            </span>
                          </td>
                          <td className="right font-semibold">{c.billCount}</td>
                          <td className="right">{c.itemsSold} units</td>
                          <td className="right font-semibold text-primary">{formatMoney(c.totalRevenue)}</td>
                          <td className="right muted">{formatMoney(c.averageBillValue)}</td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Section F: Recent Completed Sales Table */}
          <div className="card">
            <div className="card-head">
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Receipt size={18} color="var(--accent)" />
                <h3>Recent Completed Sales</h3>
              </div>
              <Link to="/sales" className="btn btn-ghost" style={{ fontSize: '0.82rem', padding: '0.35rem 0.75rem' }}>
                View All Sales History <ArrowUpRight size={14} />
              </Link>
            </div>

            {recentSales.length === 0 ? (
              <div className="empty" style={{ padding: '2rem 1.5rem', textAlign: 'center' }}>
                <p className="muted">No recent sales found.</p>
              </div>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Invoice ID</th>
                      <th>Date & Time</th>
                      <th>Cashier</th>
                      <th className="right">Units</th>
                      <th className="right">Amount</th>
                      <th>Payment Mode</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentSales.map((sale) => (
                      <tr key={sale.id}>
                        <td>
                          <Link to={`/sales/${sale.id}`} style={{ fontWeight: 600, color: 'var(--accent)' }}>
                            {sale.invoiceNumber}
                          </Link>
                        </td>
                        <td className="muted">{formatDate(sale.createdAt)}</td>
                        <td>{sale.cashierName}</td>
                        <td className="right">{sale.itemCount}</td>
                        <td className="right" style={{ fontWeight: 700, color: 'var(--navy)' }}>
                          {formatMoney(sale.totalAmount)}
                        </td>
                        <td>
                          <span className={`payment-pill ${sale.paymentMethod?.toLowerCase() || 'cash'}`}>
                            {sale.paymentMethod}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
