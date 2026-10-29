import { useState } from 'react'
import { AlertTriangle, PackagePlus } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import useDebounce from '../hooks/useDebounce'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { Button, DataTable, EmptyState, ErrorState, Field, Modal, MultiSelectCategoryFilter, PageHeader, Pagination, Spinner, StatusBadge } from '../components/ui'
import { errorMessage, formatMoney } from '../utils/format'

export default function InventoryPage() {
  const { user } = useAuth()
  const toast = useToast()
  const isAdmin = user.role === 'ADMIN'
  const [query, setQuery] = useState('')
  const search = useDebounce(query)
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([])
  const [page, setPage] = useState(0)
  const [adjusting, setAdjusting] = useState(null)
  const [busy, setBusy] = useState(false)

  const categories = useFetch(async () => (await api.get('/api/categories')).data, [])

  const categoryParam =
    selectedCategoryIds.length > 0 && selectedCategoryIds.length < (categories.data?.length ?? 0)
      ? selectedCategoryIds.join(',')
      : undefined

  const { data, loading, error, reload } = useFetch(
    async () => (await api.get('/api/inventory', { params: { search, categoryIds: categoryParam, page, size: 12 } })).data,
    [search, categoryParam, page])

  const saveStock = async (e) => {
    e.preventDefault()
    const qty = Number(adjusting.stockQuantity)
    if (!Number.isInteger(qty) || qty < 0) { toast.error('Enter a whole number, 0 or more'); return }
    setBusy(true)
    try {
      await api.patch(`/api/inventory/${adjusting.id}/stock`, { stockQuantity: qty })
      toast.success('Stock updated'); setAdjusting(null); reload()
    } catch (err) { toast.error(errorMessage(err)) }
    finally { setBusy(false) }
  }

  const lowOnPage = data?.content.filter((p) => p.stockStatus !== 'IN_STOCK').length ?? 0
  return (
    <>
      <PageHeader title="Inventory" subtitle={isAdmin ? 'View and adjust stock levels' : 'Check what is available'} />
      <div className="toolbar">
        <input className="input" placeholder="Search by name or SKU" value={query} onChange={(e) => { setQuery(e.target.value); setPage(0) }} />
        <MultiSelectCategoryFilter
          categories={categories.data || []}
          selectedIds={selectedCategoryIds}
          onChange={(ids) => {
            setSelectedCategoryIds(ids)
            setPage(0)
          }}
        />
      </div>
      {lowOnPage > 0 && <div className="alert-warn"><AlertTriangle size={18} /> {lowOnPage} product(s) on this page are low or out of stock.</div>}
      <div className="card">
        {loading ? <Spinner /> : error ? <ErrorState message={error} onRetry={reload} /> : (
          <>
            <DataTable rows={data.content} empty={<EmptyState title="No products found" text="Try a different search." />}
              columns={[
                { header: 'Product', render: (p) => <strong>{p.name}</strong> },
                { header: 'SKU', render: (p) => p.sku },
                { header: 'Category', render: (p) => p.categoryName },
                { header: 'Price', render: (p) => formatMoney(p.sellingPrice) },
                { header: 'Available', render: (p) => <strong>{p.stockQuantity}</strong> },
                { header: 'Status', render: (p) => <StatusBadge status={p.stockStatus} /> },
                ...(isAdmin ? [{ header: 'Manage', className: 'right', render: (p) => (
                  <button className="icon-btn" onClick={() => setAdjusting({ ...p })} aria-label={`Update stock for ${p.name}`}><PackagePlus size={17} /></button>) }] : []),
              ]} />
            <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
          </>
        )}
      </div>
      {adjusting && (
        <Modal title={`Update stock: ${adjusting.name}`} onClose={() => setAdjusting(null)}>
          <form onSubmit={saveStock}>
            <Field label="Stock quantity"><input className="input" type="number" min="0" value={adjusting.stockQuantity} onChange={(e) => setAdjusting({ ...adjusting, stockQuantity: e.target.value })} /></Field>
            <div className="modal-actions"><Button variant="ghost" type="button" onClick={() => setAdjusting(null)}>Cancel</Button><Button type="submit" loading={busy}>Save stock</Button></div>
          </form>
        </Modal>
      )}
    </>
  )
}
