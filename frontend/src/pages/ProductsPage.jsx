import { useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import useDebounce from '../hooks/useDebounce'
import { useToast } from '../context/ToastContext'
import { Button, ConfirmDialog, DataTable, EmptyState, ErrorState, Field, Modal, MultiSelectCategoryFilter, PageHeader, Pagination, Spinner, StatusBadge } from '../components/ui'
import { errorMessage, fieldErrors, formatMoney } from '../utils/format'

const EMPTY = { name: '', sku: '', description: '', sellingPrice: '', costPrice: '', stockQuantity: 0, lowStockThreshold: 5, categoryId: '' }

export default function ProductsPage() {
  const toast = useToast()
  const [query, setQuery] = useState('')
  const search = useDebounce(query)
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([])
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(null)      // null | product-form object
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState({})

  const categories = useFetch(async () => (await api.get('/api/categories')).data, [])

  const categoryParam =
    selectedCategoryIds.length > 0 && selectedCategoryIds.length < (categories.data?.length ?? 0)
      ? selectedCategoryIds.join(',')
      : undefined

  const products = useFetch(
    async () => (await api.get('/api/products', { params: { search, categoryIds: categoryParam, page, size: 10 } })).data,
    [search, categoryParam, page]
  )

  const openNew = () => { setErrors({}); setEditing({ ...EMPTY, categoryId: categories.data?.[0]?.id ?? '' }) }
  const openEdit = (p) => { setErrors({}); setEditing({ ...p, description: p.description ?? '', costPrice: p.costPrice ?? '' }) }
  const set = (k) => (e) => setEditing({ ...editing, [k]: e.target.value })

  const save = async (e) => {
    e.preventDefault()
    const errs = {}
    if (!editing.name.trim()) errs.name = 'Product name is required'
    if (!editing.sku.trim()) errs.sku = 'SKU is required'
    if (!(Number(editing.sellingPrice) > 0)) errs.sellingPrice = 'Price must be greater than 0'
    if (Number(editing.stockQuantity) < 0 || editing.stockQuantity === '') errs.stockQuantity = 'Stock cannot be negative'
    if (!editing.categoryId) errs.categoryId = 'Choose a category'
    setErrors(errs)
    if (Object.keys(errs).length) return
    const body = {
      name: editing.name, sku: editing.sku, description: editing.description,
      sellingPrice: Number(editing.sellingPrice), costPrice: editing.costPrice === '' ? null : Number(editing.costPrice),
      stockQuantity: Number(editing.stockQuantity), lowStockThreshold: Number(editing.lowStockThreshold), categoryId: Number(editing.categoryId),
    }
    setBusy(true)
    try {
      if (editing.id) await api.put(`/api/products/${editing.id}`, body)
      else await api.post('/api/products', body)
      toast.success(editing.id ? 'Product updated' : 'Product added')
      setEditing(null); products.reload()
    } catch (err) {
      setErrors(fieldErrors(err)); toast.error(errorMessage(err))
    } finally { setBusy(false) }
  }

  const remove = async () => {
    setBusy(true)
    try {
      await api.delete(`/api/products/${deleting.id}`)
      toast.success('Product deleted'); setDeleting(null); products.reload()
    } catch (err) { toast.error(errorMessage(err)); setDeleting(null) }
    finally { setBusy(false) }
  }

  const page_ = products.data
  return (
    <>
      <PageHeader title="Products" subtitle="Add and manage everything you sell">
        <Button onClick={openNew} disabled={!categories.data?.length}><Plus size={16} /> Add product</Button>
      </PageHeader>
      {categories.data && !categories.data.length && <div className="alert-warn">Create a category first, then you can add products.</div>}
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
      <div className="card">
        {products.loading ? <Spinner /> : products.error ? <ErrorState message={products.error} onRetry={products.reload} /> : (
          <>
            <DataTable rows={page_.content} empty={<EmptyState title="No products found" text="Try a different search or add a new product." />}
              columns={[
                { header: 'Name', render: (p) => <><strong>{p.name}</strong><br /><small className="muted">{p.sku}</small></> },
                { header: 'Category', render: (p) => p.categoryName },
                { header: 'Price', render: (p) => formatMoney(p.sellingPrice) },
                { header: 'Stock', render: (p) => p.stockQuantity },
                { header: 'Status', render: (p) => <StatusBadge status={p.stockStatus} /> },
                { header: 'Actions', className: 'right', render: (p) => (
                  <div className="row-actions">
                    <button className="icon-btn" onClick={() => openEdit(p)} aria-label={`Edit ${p.name}`}><Pencil size={16} /></button>
                    <button className="icon-btn" onClick={() => setDeleting(p)} aria-label={`Delete ${p.name}`}><Trash2 size={16} /></button>
                  </div>) },
              ]} />
            <Pagination page={page} totalPages={page_.totalPages} onChange={setPage} />
          </>
        )}
      </div>

      {editing && (
        <Modal title={editing.id ? 'Edit product' : 'Add product'} onClose={() => setEditing(null)}>
          <form onSubmit={save} noValidate>
            <div className="form-grid">
              <div className="span-2"><Field label="Product name" error={errors.name}><input className="input" value={editing.name} onChange={set('name')} /></Field></div>
              <Field label="SKU / product code" error={errors.sku}><input className="input" value={editing.sku} onChange={set('sku')} /></Field>
              <Field label="Category" error={errors.categoryId}>
                <select className="input" value={editing.categoryId} onChange={set('categoryId')}>
                  <option value="">Select…</option>
                  {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
              <Field label="Selling price (₹)" error={errors.sellingPrice}><input className="input" type="number" step="0.01" min="0" value={editing.sellingPrice} onChange={set('sellingPrice')} /></Field>
              <Field label="Cost price (₹, optional)" error={errors.costPrice}><input className="input" type="number" step="0.01" min="0" value={editing.costPrice} onChange={set('costPrice')} /></Field>
              <Field label="Stock quantity" error={errors.stockQuantity}><input className="input" type="number" min="0" value={editing.stockQuantity} onChange={set('stockQuantity')} /></Field>
              <Field label="Low-stock threshold" error={errors.lowStockThreshold}><input className="input" type="number" min="0" value={editing.lowStockThreshold} onChange={set('lowStockThreshold')} /></Field>
              <div className="span-2"><Field label="Description (optional)" error={errors.description}><textarea className="input" rows={2} value={editing.description} onChange={set('description')} /></Field></div>
            </div>
            <div className="modal-actions">
              <Button variant="ghost" type="button" onClick={() => setEditing(null)}>Cancel</Button>
              <Button type="submit" loading={busy}>Save product</Button>
            </div>
          </form>
        </Modal>
      )}
      {deleting && <ConfirmDialog title="Delete product" message={`Delete "${deleting.name}"? This cannot be undone.`} loading={busy} onConfirm={remove} onCancel={() => setDeleting(null)} />}
    </>
  )
}
