import { Link } from 'react-router-dom'
import { Package, Tags, Receipt, IndianRupee, Plus, Boxes } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import { Button, DataTable, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge } from '../components/ui'
import { formatMoney } from '../utils/format'

export default function AdminDashboard() {
  const { data, loading, error, reload } = useFetch(async () => {
    const [summary, low, recent] = await Promise.all([
      api.get('/api/dashboard/summary'), api.get('/api/dashboard/low-stock'), api.get('/api/dashboard/recent-sales'),
    ])
    return { summary: summary.data, low: low.data, recent: recent.data }
  })

  if (loading) return <Spinner />
  if (error) return <ErrorState message={error} onRetry={reload} />
  const { summary, low, recent } = data
  const stats = [
    { label: 'Total Products', value: summary.totalProducts, icon: Package },
    { label: 'Total Categories', value: summary.totalCategories, icon: Tags },
    { label: 'Total Sales', value: summary.totalSales, icon: Receipt },
    { label: 'Total Revenue', value: formatMoney(summary.totalRevenue), icon: IndianRupee },
  ]

  return (
    <>
      <PageHeader title="Dashboard" subtitle="How the shop is doing right now">
        <Link to="/admin/products"><Button variant="ghost"><Plus size={16} /> Add product</Button></Link>
        <Link to="/inventory"><Button variant="ghost"><Boxes size={16} /> View inventory</Button></Link>
        <Link to="/billing"><Button><Receipt size={16} /> Open billing</Button></Link>
      </PageHeader>
      <div className="grid-4">
        {stats.map(({ label, value, icon: Icon }) => (
          <div className="card stat" key={label}>
            <div className="stat-icon"><Icon size={20} /></div>
            <div><div className="stat-value">{value}</div><div className="stat-label">{label}</div></div>
          </div>
        ))}
      </div>
      <div className="grid-2">
        <div className="card">
          <div className="card-head"><h2>Low-stock products ({low.length})</h2></div>
          <DataTable rows={low} empty={<EmptyState title="Stock looks healthy" text="No product is at or below its threshold." />}
            columns={[
              { header: 'Product', render: (p) => <>{p.name}<br /><small className="muted">{p.sku}</small></> },
              { header: 'Stock', render: (p) => `${p.stockQuantity} / ${p.lowStockThreshold}` },
              { header: 'Status', render: (p) => <StatusBadge status={p.stockStatus} /> },
            ]} />
        </div>
        <div className="card">
          <div className="card-head"><h2>Recent sales</h2><Link to="/sales">View all</Link></div>
          <DataTable rows={recent} empty={<EmptyState title="No sales yet" text="Bills you generate will show up here." action={<Link to="/billing" className="btn btn-primary">Create a bill</Link>} />}
            columns={[
              { header: 'Invoice', render: (s) => <Link to={`/sales/${s.id}`}>{s.invoiceNumber}</Link> },
              { header: 'Cashier', render: (s) => s.cashierName },
              { header: 'Total', className: 'right', render: (s) => formatMoney(s.totalAmount) },
            ]} />
        </div>
      </div>
    </>
  )
}
