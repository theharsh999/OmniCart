import { Link } from 'react-router-dom'
import { Receipt, IndianRupee, Search } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import { Button, DataTable, EmptyState, ErrorState, PageHeader, Spinner } from '../components/ui'
import { formatDate, formatMoney } from '../utils/format'
import { useAuth } from '../context/AuthContext'

export default function StaffDashboard() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useFetch(async () => {
    const [summary, recent] = await Promise.all([api.get('/api/dashboard/staff-summary'), api.get('/api/dashboard/recent-sales')])
    return { summary: summary.data, recent: recent.data }
  })

  if (loading) return <Spinner />
  if (error) return <ErrorState message={error} onRetry={reload} />
  const { summary, recent } = data

  return (
    <>
      <PageHeader title={`Hello, ${user.name}`} subtitle="Your sales for today">
        <Link to="/inventory"><Button variant="ghost"><Search size={16} /> Find products</Button></Link>
        <Link to="/billing"><Button><Receipt size={16} /> Create bill</Button></Link>
      </PageHeader>
      <div className="grid-2">
        <div className="card stat"><div className="stat-icon"><Receipt size={20} /></div><div><div className="stat-value">{summary.todaySalesCount}</div><div className="stat-label">Bills today</div></div></div>
        <div className="card stat"><div className="stat-icon"><IndianRupee size={20} /></div><div><div className="stat-value">{formatMoney(summary.todayRevenue)}</div><div className="stat-label">Sales today</div></div></div>
      </div>
      <div className="card">
        <div className="card-head"><h2>Your recent sales</h2><Link to="/sales">View all</Link></div>
        <DataTable rows={recent} empty={<EmptyState title="No sales yet" text="Create your first bill to see it here." />}
          columns={[
            { header: 'Invoice', render: (s) => <Link to={`/sales/${s.id}`}>{s.invoiceNumber}</Link> },
            { header: 'Date', render: (s) => formatDate(s.createdAt) },
            { header: 'Total', className: 'right', render: (s) => formatMoney(s.totalAmount) },
          ]} />
      </div>
    </>
  )
}
