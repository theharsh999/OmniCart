import { useState } from 'react'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import api from '../services/api'
import useFetch from '../hooks/useFetch'
import { useToast } from '../context/ToastContext'
import { Button, ConfirmDialog, DataTable, EmptyState, ErrorState, Field, Modal, PageHeader, Spinner } from '../components/ui'
import { errorMessage, fieldErrors } from '../utils/format'

export default function CategoriesPage() {
  const toast = useToast()
  const { data, loading, error, reload } = useFetch(async () => (await api.get('/api/categories')).data, [])
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [busy, setBusy] = useState(false)
  const [errors, setErrors] = useState({})

  const save = async (e) => {
    e.preventDefault()
    if (!editing.name.trim()) { setErrors({ name: 'Category name is required' }); return }
    setBusy(true)
    try {
      const body = { name: editing.name, description: editing.description }
      if (editing.id) await api.put(`/api/categories/${editing.id}`, body)
      else await api.post('/api/categories', body)
      toast.success(editing.id ? 'Category updated' : 'Category added')
      setEditing(null); reload()
    } catch (err) { setErrors(fieldErrors(err)); toast.error(errorMessage(err)) }
    finally { setBusy(false) }
  }

  const remove = async () => {
    setBusy(true)
    try { await api.delete(`/api/categories/${deleting.id}`); toast.success('Category deleted'); reload() }
    catch (err) { toast.error(errorMessage(err)) }
    finally { setBusy(false); setDeleting(null) }
  }

  return (
    <>
      <PageHeader title="Categories" subtitle="Group your products">
        <Button onClick={() => { setErrors({}); setEditing({ name: '', description: '' }) }}><Plus size={16} /> Add category</Button>
      </PageHeader>
      <div className="card">
        {loading ? <Spinner /> : error ? <ErrorState message={error} onRetry={reload} /> : (
          <DataTable rows={data} empty={<EmptyState title="No categories yet" text="Add a category such as Groceries or Stationery." />}
            columns={[
              { header: 'Name', render: (c) => <strong>{c.name}</strong> },
              { header: 'Description', render: (c) => c.description || '—' },
              { header: 'Products', render: (c) => c.productCount },
              { header: 'Actions', className: 'right', render: (c) => (
                <div className="row-actions">
                  <button className="icon-btn" onClick={() => { setErrors({}); setEditing({ ...c, description: c.description ?? '' }) }} aria-label={`Edit ${c.name}`}><Pencil size={16} /></button>
                  <button className="icon-btn" onClick={() => setDeleting(c)} aria-label={`Delete ${c.name}`}><Trash2 size={16} /></button>
                </div>) },
            ]} />
        )}
      </div>
      {editing && (
        <Modal title={editing.id ? 'Edit category' : 'Add category'} onClose={() => setEditing(null)}>
          <form onSubmit={save} noValidate>
            <Field label="Category name" error={errors.name}><input className="input" value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} /></Field>
            <Field label="Description (optional)" error={errors.description}><input className="input" value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} /></Field>
            <div className="modal-actions"><Button variant="ghost" type="button" onClick={() => setEditing(null)}>Cancel</Button><Button type="submit" loading={busy}>Save category</Button></div>
          </form>
        </Modal>
      )}
      {deleting && <ConfirmDialog title="Delete category" message={deleting.productCount > 0 ? `"${deleting.name}" still has ${deleting.productCount} product(s). Move them to another category first.` : `Delete "${deleting.name}"?`} loading={busy} onConfirm={remove} onCancel={() => setDeleting(null)} />}
    </>
  )
}
