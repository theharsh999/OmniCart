import { useState, useMemo } from 'react'
import { formatMoney } from '../utils/format'
import { TrendingUp, ShoppingBag, Calendar, Info } from 'lucide-react'

export default function RevenueTrendChart({ data = [] }) {
  const [metric, setMetric] = useState('revenue') // 'revenue' | 'orders'
  const [hoveredIndex, setHoveredIndex] = useState(null)

  const chartData = useMemo(() => {
    if (!Array.isArray(data) || data.length === 0) return []
    return data.map((d) => ({
      date: d.date,
      label: d.dayLabel || d.date,
      revenue: Number(d.revenue || 0),
      orderCount: Number(d.orderCount || 0),
      itemsSold: Number(d.itemsSold || 0),
    }))
  }, [data])

  const { maxVal, yTicks } = useMemo(() => {
    if (chartData.length === 0) return { maxVal: 100, yTicks: [0, 50, 100] }
    const rawMax = Math.max(...chartData.map((d) => (metric === 'revenue' ? d.revenue : d.orderCount)), 0)
    if (rawMax <= 0) return { maxVal: metric === 'revenue' ? 1000 : 5, yTicks: metric === 'revenue' ? [0, 500, 1000] : [0, 2, 5] }

    // Round max to comfortable tick step
    const step = metric === 'revenue'
      ? Math.ceil(rawMax / 4 / 100) * 100 || 50
      : Math.ceil(rawMax / 4) || 1
    const computedMax = step * 4
    return {
      maxVal: computedMax,
      yTicks: [0, step, step * 2, step * 3, computedMax],
    }
  }, [chartData, metric])

  const W = 800
  const H = 230
  const padL = 60
  const padR = 20
  const padT = 20
  const padB = 40
  const plotW = W - padL - padR
  const plotH = H - padT - padB

  const points = useMemo(() => {
    if (chartData.length === 0) return []
    const n = chartData.length
    return chartData.map((d, i) => {
      const x = padL + (n === 1 ? plotW / 2 : (i / (n - 1)) * plotW)
      const val = metric === 'revenue' ? d.revenue : d.orderCount
      const y = padT + plotH - (val / (maxVal || 1)) * plotH
      return { ...d, x, y, val }
    })
  }, [chartData, metric, maxVal, plotW, plotH, padL, padT])

  const { pathLine, pathArea } = useMemo(() => {
    if (points.length === 0) return { pathLine: '', pathArea: '' }
    if (points.length === 1) {
      const p = points[0]
      return {
        pathLine: `M ${padL} ${p.y} L ${padL + plotW} ${p.y}`,
        pathArea: `M ${padL} ${padT + plotH} L ${padL} ${p.y} L ${padL + plotW} ${p.y} L ${padL + plotW} ${padT + plotH} Z`,
      }
    }

    // Monotonic cubic bezier curve
    let d = `M ${points[0].x} ${points[0].y}`
    for (let i = 0; i < points.length - 1; i++) {
      const curr = points[i]
      const next = points[i + 1]
      const cpX1 = curr.x + (next.x - curr.x) / 3
      const cpX2 = curr.x + (2 * (next.x - curr.x)) / 3
      d += ` C ${cpX1} ${curr.y}, ${cpX2} ${next.y}, ${next.x} ${next.y}`
    }

    const area = `${d} L ${points[points.length - 1].x} ${padT + plotH} L ${points[0].x} ${padT + plotH} Z`
    return { pathLine: d, pathArea: area }
  }, [points, padL, padT, plotH, plotW])

  // Subsample X labels if data is dense (> 12 points)
  const labelInterval = useMemo(() => {
    if (chartData.length <= 10) return 1
    if (chartData.length <= 20) return 2
    return Math.ceil(chartData.length / 8)
  }, [chartData.length])

  const activePoint = hoveredIndex !== null && points[hoveredIndex] ? points[hoveredIndex] : null

  const totalPeriodRevenue = useMemo(() => chartData.reduce((acc, d) => acc + d.revenue, 0), [chartData])
  const totalPeriodOrders = useMemo(() => chartData.reduce((acc, d) => acc + d.orderCount, 0), [chartData])

  // Derive dynamic business insights exclusively from real data
  const trendInsight = useMemo(() => {
    if (chartData.length === 0) return null
    const nonZeroDays = chartData.filter((d) => d.revenue > 0)
    const zeroDays = chartData.filter((d) => d.revenue === 0)

    if (nonZeroDays.length === 0) {
      return {
        hasData: false,
        message: 'No revenue-generating sales recorded in this period.',
      }
    }

    const highestDay = nonZeroDays.reduce((max, d) => (d.revenue > max.revenue ? d : max), nonZeroDays[0])
    let msg = `Highest revenue day: ${highestDay.label} (${highestDay.date}) with ${formatMoney(highestDay.revenue)} across ${highestDay.orderCount} ${highestDay.orderCount === 1 ? 'bill' : 'bills'}.`
    if (zeroDays.length > 0) {
      msg += ` (${zeroDays.length} ${zeroDays.length === 1 ? 'day' : 'days'} with zero sales in this period).`
    }

    return {
      hasData: true,
      message: msg,
    }
  }, [chartData])

  return (
    <div className="card chart-card">
      <div className="chart-header">
        <div className="chart-title-area">
          <h3>Revenue & Order Trends</h3>
          <p className="muted chart-sub-text">
            Total: <strong>{formatMoney(totalPeriodRevenue)}</strong> • {totalPeriodOrders} {totalPeriodOrders === 1 ? 'bill' : 'bills'} across {chartData.length} days
          </p>
        </div>

        <div className="chart-controls">
          <div className="chart-tab-group">
            <button
              className={`chart-tab-btn ${metric === 'revenue' ? 'active' : ''}`}
              onClick={() => setMetric('revenue')}
            >
              <TrendingUp size={14} /> Revenue
            </button>
            <button
              className={`chart-tab-btn ${metric === 'orders' ? 'active' : ''}`}
              onClick={() => setMetric('orders')}
            >
              <ShoppingBag size={14} /> Orders
            </button>
          </div>
        </div>
      </div>

      {trendInsight && (
        <div className="analytics-insight-banner compact" style={{ margin: '0 1.25rem 0.75rem 1.25rem' }}>
          <TrendingUp size={14} color="#4f46e5" style={{ flexShrink: 0 }} />
          <div>
            <strong>Trend Insight:</strong> {trendInsight.message}
          </div>
        </div>
      )}

      <div className="chart-wrapper">
        <svg
          viewBox={`0 0 ${W} ${H}`}
          className="analytics-svg"
          onMouseLeave={() => setHoveredIndex(null)}
        >
          <defs>
            <linearGradient id="revenueGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#4f46e5" stopOpacity="0.32" />
              <stop offset="85%" stopColor="#4f46e5" stopOpacity="0.02" />
              <stop offset="100%" stopColor="#4f46e5" stopOpacity="0.0" />
            </linearGradient>
            <linearGradient id="ordersGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#0891b2" stopOpacity="0.30" />
              <stop offset="85%" stopColor="#0891b2" stopOpacity="0.02" />
              <stop offset="100%" stopColor="#0891b2" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Horizontal gridlines */}
          {yTicks.map((tickVal, idx) => {
            const y = padT + plotH - (tickVal / (maxVal || 1)) * plotH
            return (
              <g key={idx}>
                <line
                  x1={padL}
                  y1={y}
                  x2={W - padR}
                  y2={y}
                  stroke="#e2e8f0"
                  strokeDasharray={idx === 0 ? 'none' : '4 4'}
                  strokeWidth="1"
                />
                <text
                  x={padL - 10}
                  y={y + 4}
                  textAnchor="end"
                  fontSize="11"
                  fill="#94a3b8"
                  fontWeight="500"
                >
                  {metric === 'revenue' ? (tickVal >= 1000 ? `₹${(tickVal / 1000).toFixed(tickVal % 1000 === 0 ? 0 : 1)}k` : `₹${tickVal}`) : tickVal}
                </text>
              </g>
            )
          })}

          {/* Base Area & Line */}
          {pathArea && (
            <path
              d={pathArea}
              fill={metric === 'revenue' ? 'url(#revenueGradient)' : 'url(#ordersGradient)'}
            />
          )}

          {pathLine && (
            <path
              d={pathLine}
              fill="none"
              stroke={metric === 'revenue' ? '#4f46e5' : '#0891b2'}
              strokeWidth="2.75"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          )}

          {/* Interactive Crosshair Line */}
          {activePoint && (
            <line
              x1={activePoint.x}
              y1={padT}
              x2={activePoint.x}
              y2={padT + plotH}
              stroke="#6366f1"
              strokeWidth="1.5"
              strokeDasharray="3 3"
            />
          )}

          {/* Data Points */}
          {points.map((p, idx) => {
            const isHovered = hoveredIndex === idx
            return (
              <circle
                key={idx}
                cx={p.x}
                cy={p.y}
                r={isHovered ? 6 : points.length > 20 ? 2.5 : 4}
                fill={metric === 'revenue' ? '#4f46e5' : '#0891b2'}
                stroke="#ffffff"
                strokeWidth={isHovered ? 2.5 : 1.5}
                className="chart-point-dot"
                style={{
                  cursor: 'pointer',
                  filter: isHovered ? 'drop-shadow(0 2px 6px rgba(79, 70, 229, 0.4))' : 'none',
                }}
                onMouseEnter={() => setHoveredIndex(idx)}
              />
            )
          })}

          {/* X Axis Labels */}
          {points.map((p, idx) => {
            if (idx % labelInterval !== 0 && idx !== points.length - 1) return null
            return (
              <text
                key={idx}
                x={p.x}
                y={padT + plotH + 20}
                textAnchor="middle"
                fontSize="11"
                fill="#64748b"
                fontWeight="500"
              >
                {p.label}
              </text>
            )
          })}

          {/* Invisible overlay columns for smooth mouse tracking */}
          {points.map((p, idx) => {
            const colW = plotW / (points.length || 1)
            const leftX = p.x - colW / 2
            return (
              <rect
                key={idx}
                x={leftX}
                y={padT}
                width={colW}
                height={plotH}
                fill="transparent"
                onMouseEnter={() => setHoveredIndex(idx)}
              />
            )
          })}
        </svg>

        {/* Floating Tooltip Card */}
        {activePoint && (
          <div
            className="chart-tooltip"
            style={{
              left: `${(activePoint.x / W) * 100}%`,
              top: `${(activePoint.y / H) * 100}%`,
            }}
          >
            <div className="tooltip-date">
              <Calendar size={12} /> {activePoint.date} ({activePoint.label})
            </div>
            <div className="tooltip-row">
              <span className="tooltip-label">Daily Revenue:</span>
              <strong className="tooltip-val primary">{formatMoney(activePoint.revenue)}</strong>
            </div>
            <div className="tooltip-row">
              <span className="tooltip-label">Bills Generated:</span>
              <span className="tooltip-val">{activePoint.orderCount} orders</span>
            </div>
            <div className="tooltip-row">
              <span className="tooltip-label">Items Sold:</span>
              <span className="tooltip-val">{activePoint.itemsSold} units</span>
            </div>
          </div>
        )}
      </div>

      <div className="chart-footer-note">
        <Info size={13} /> Days with zero sales are plotted at ₹0.00. Hover over any date to inspect details.
      </div>
    </div>
  )
}
